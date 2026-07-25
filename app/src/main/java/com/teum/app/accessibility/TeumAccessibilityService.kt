package com.teum.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.teum.app.core.model.InterventionMode
import com.teum.app.debug.TeumLogger
import com.teum.app.data.repository.SessionLogRepository
import com.teum.app.data.repository.TargetAppRepository
import com.teum.app.data.repository.UserSettingsRepository
import com.teum.app.data.repository.VulnerableTimeRepository
import com.teum.app.overlay.BrakeChoice
import com.teum.app.overlay.IntentChoice
import com.teum.app.overlay.IntentCheckMode
import com.teum.app.overlay.OverlayController
import com.teum.app.overlay.TargetDurationChoice
import com.teum.app.session.AppSession
import com.teum.app.session.OutcomeType
import com.teum.app.session.ReopenCheckResult
import com.teum.app.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TeumAccessibilityService : AccessibilityService() {
    private val targetAppRepository by lazy {
        TargetAppRepository(applicationContext)
    }
    private val overlayController by lazy {
        OverlayController(this)
    }
    private val sessionLogRepository by lazy {
        SessionLogRepository(applicationContext)
    }
    private val userSettingsRepository by lazy {
        UserSettingsRepository(applicationContext)
    }
    private val vulnerableTimeRepository by lazy {
        VulnerableTimeRepository(applicationContext)
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val brakeHandler = Handler(Looper.getMainLooper())
    private val brakeRunnable = Runnable {
        showSessionBrakeIfNeeded()
    }

    private var currentForegroundPackage: String? = null
    private var activeTargetPackage: String? = null
    private var sessionNeedsIntentCheck: Boolean = false
    private var intentCheckedForCurrentSession: Boolean = false
    private var brakeSuppressedForCurrentSession: Boolean = false
    private var currentEntryTimeMillis: Long? = null
    private var currentReopenCheckResult: ReopenCheckResult? = null
    private var currentDebugSessionId: Long? = null
    private var currentPolicySnapshot: PolicySnapshot = PolicySnapshot()
    private var pendingOutcome: PendingOutcome? = null
    private var latestObservedPackage: String? = null
    private var pendingEnterPackage: String? = null
    private var pendingEnterRunnable: Runnable? = null
    private var ignoreTargetEnterUntilMillis: Long = 0L
    private val suppressReentryUntilByPackage = mutableMapOf<String, Long>()
    private val suppressReentryReasonByPackage = mutableMapOf<String, String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val accessibilityEvent = event ?: return
        if (accessibilityEvent.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            accessibilityEvent.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val packageName = accessibilityEvent.packageName?.toString() ?: return
        if (packageName == ownPackageName()) return
        latestObservedPackage = packageName
        if (packageName == currentForegroundPackage) {
            restoreIntentCheckIfNeeded(packageName)
            return
        }

        handleForegroundPackageChanged(packageName)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        cancelBrakeSchedule()
        cancelPendingTargetEnter(reason = "service_destroyed", currentPackage = latestObservedPackage)
        serviceScope.cancel()
        overlayController.removeOverlayIfAttached()
        super.onDestroy()
    }

    private fun handleForegroundPackageChanged(packageName: String) {
        val previousPackage = currentForegroundPackage

        if (targetAppRepository.isTargetPackage(packageName)) {
            if (previousPackage != null &&
                previousPackage != packageName &&
                targetAppRepository.isTargetPackage(previousPackage)
            ) {
                handleConfirmedTargetExit(previousPackage)
            }
            scheduleStableTargetEnter(packageName)
            return
        }

        cancelPendingTargetEnter(reason = "package_changed", currentPackage = packageName)
        currentForegroundPackage = packageName
        if (previousPackage != null && targetAppRepository.isTargetPackage(previousPackage)) {
            handleConfirmedTargetExit(previousPackage)
        } else {
            resetIntentCheckSession()
        }
    }

    private fun handleConfirmedTargetExit(packageName: String) {
        Log.d(TAG, "target app exited: $packageName")
        TeumLogger.access("EXIT", packageName)
        cancelBrakeSchedule()
        var endedSession: AppSession? = null
        if (SessionManager.hasActiveSessionFor(packageName)) {
            if (overlayController.currentOverlayName == "SESSION_BRAKE") {
                SessionManager.markInterventionHidden()
            }
            endedSession = SessionManager.endSession(
                packageName = packageName,
                reason = "target_exit"
            )
            if (endedSession != null) {
                suppressReentry(
                    packageName = packageName,
                    reason = "after_target_exit",
                    durationMillis = SUPPRESS_REENTRY_AFTER_TARGET_EXIT_MILLIS
                )
            }
        }
        resetIntentCheckSession()
        endedSession?.let(::handleTargetExitEndedSession)
    }

    private fun scheduleStableTargetEnter(packageName: String) {
        if (shouldIgnoreEnterForOutcome(packageName)) return
        if (shouldIgnoreEnterForHomeNavigation(packageName)) return

        pendingEnterPackage?.let { pendingPackage ->
            if (pendingPackage != packageName) {
                cancelPendingTargetEnter(reason = "package_changed", currentPackage = packageName)
            }
        }

        TeumLogger.flow("[ACCESS] ENTER_CANDIDATE package=$packageName")
        pendingEnterPackage = packageName
        pendingEnterRunnable?.let(brakeHandler::removeCallbacks)
        val runnable = Runnable {
            confirmStableTargetEnter(packageName)
        }
        pendingEnterRunnable = runnable
        TeumLogger.flow(
            "[ACCESS] ENTER_CONFIRM_SCHEDULED package=$packageName delay=$ENTER_CONFIRM_DELAY_MILLIS"
        )
        brakeHandler.postDelayed(runnable, ENTER_CONFIRM_DELAY_MILLIS)
    }

    private fun confirmStableTargetEnter(packageName: String) {
        if (pendingEnterPackage != packageName) return
        pendingEnterPackage = null
        pendingEnterRunnable = null

        if (shouldIgnoreEnterForOutcome(packageName)) return
        if (shouldIgnoreEnterForHomeNavigation(packageName)) return

        val latestPackage = currentStablePackageName()
        if (latestPackage != packageName) {
            TeumLogger.flow(
                "[ACCESS] ENTER_IGNORED_NOT_STABLE package=$packageName current=$latestPackage"
            )
            return
        }

        if (shouldSuppressReentry(packageName)) return

        val entryTimeMillis = System.currentTimeMillis()
        serviceScope.launch {
            val policySnapshot = resolvePolicySnapshot(entryTimeMillis)
            brakeHandler.post {
                if (currentStablePackageName() != packageName) {
                    TeumLogger.flow(
                        "[ACCESS] ENTER_IGNORED_NOT_STABLE package=$packageName current=${currentStablePackageName()}"
                    )
                    return@post
                }

                currentForegroundPackage = packageName
                currentPolicySnapshot = policySnapshot
                TeumLogger.flow("[ACCESS] ENTER_CONFIRMED package=$packageName")
                Log.d(TAG, "target app entered: $packageName")
                TeumLogger.access("ENTER", packageName)
                saveAppOpenEvent(
                    packageName = packageName,
                    detectedAtMillis = entryTimeMillis
                )
                val reopenCheckResult = SessionManager.checkFastReopen(
                    packageName = packageName,
                    currentEntryTimeMillis = entryTimeMillis
                )
                startIntentCheckSession(
                    packageName = packageName,
                    entryTimeMillis = entryTimeMillis,
                    reopenCheckResult = reopenCheckResult
                )
            }
        }
    }

    private fun cancelPendingTargetEnter(reason: String, currentPackage: String?) {
        val pendingPackage = pendingEnterPackage ?: return
        pendingEnterRunnable?.let(brakeHandler::removeCallbacks)
        pendingEnterPackage = null
        pendingEnterRunnable = null
        TeumLogger.flow(
            "[ACCESS] ENTER_CANCELLED package=$pendingPackage reason=$reason current=$currentPackage"
        )
    }

    private fun shouldIgnoreEnterForOutcome(packageName: String): Boolean {
        if (overlayController.currentOverlayName != "OUTCOME_CHECK") return false
        TeumLogger.flow("[ACCESS] ENTER_IGNORED package=$packageName reason=outcome_check_showing")
        return true
    }

    private fun shouldIgnoreEnterForHomeNavigation(packageName: String): Boolean {
        val nowMillis = System.currentTimeMillis()
        if (nowMillis >= ignoreTargetEnterUntilMillis) return false
        val remainingMillis = ignoreTargetEnterUntilMillis - nowMillis
        TeumLogger.flow(
            "[ACCESS] ENTER_IGNORED package=$packageName reason=home_navigation_guard remainingMs=$remainingMillis"
        )
        return true
    }

    private fun startIntentCheckSession(
        packageName: String,
        entryTimeMillis: Long,
        reopenCheckResult: ReopenCheckResult
    ) {
        activeTargetPackage = packageName
        currentEntryTimeMillis = entryTimeMillis
        currentReopenCheckResult = reopenCheckResult
        currentDebugSessionId = SessionManager.createDebugSessionId()
        currentDebugSessionId?.let { debugSessionId ->
            TeumLogger.session(
                debugSessionId = debugSessionId,
                event = "ENTER",
                detail = "package=$packageName mode=${if (reopenCheckResult.isFastReopen) IntentCheckMode.FAST_REOPEN else IntentCheckMode.NORMAL}"
            )
        }
        sessionNeedsIntentCheck = true
        intentCheckedForCurrentSession = false
        brakeSuppressedForCurrentSession = false
        showIntentCheckIfNeeded(packageName)
    }

    private fun restoreIntentCheckIfNeeded(packageName: String) {
        if (packageName != activeTargetPackage) return
        showIntentCheckIfNeeded(packageName)
    }

    private fun showIntentCheckIfNeeded(packageName: String) {
        if (!sessionNeedsIntentCheck) return
        if (intentCheckedForCurrentSession) return
        if (overlayController.overlayShowing) {
            TeumLogger.overlay(
                event = "SHOW_INTENT_SKIPPED",
                detail = "package=$packageName reason=overlay_already_showing current=${overlayController.currentOverlayName}"
            )
            return
        }

        val intentCheckMode = if (currentReopenCheckResult?.isFastReopen == true) {
            IntentCheckMode.FAST_REOPEN
        } else {
            IntentCheckMode.NORMAL
        }
        val intentCheckSource = if (intentCheckMode == IntentCheckMode.FAST_REOPEN) {
            "fast_reopen_enter"
        } else {
            "target_enter"
        }

        overlayController.showIntentCheck(
            packageName = packageName,
            mode = intentCheckMode,
            reopenGapMillis = currentReopenCheckResult?.gapMillis,
            interventionActive = currentPolicySnapshot.interventionActive,
            availableDurations = availableDurationsForPolicy(currentPolicySnapshot),
            debugSessionId = currentDebugSessionId,
            source = intentCheckSource,
            onIntentConfirmed = { intentChoice, targetDurationMillis ->
                val entryTimeMillis = currentEntryTimeMillis ?: System.currentTimeMillis()
                val reopenCheckResult = currentReopenCheckResult
                val policySnapshot = currentPolicySnapshot
                SessionManager.startSession(
                    packageName = packageName,
                    intentChoice = intentChoice,
                    targetDurationMillis = targetDurationMillis,
                    entryDetectedAtMillis = entryTimeMillis,
                    isFastReopen = reopenCheckResult?.isFastReopen == true,
                    reopenGapMillis = reopenCheckResult?.gapMillis,
                    modeAtStart = policySnapshot.mode.name,
                    isVulnerableTimeAtStart = policySnapshot.isVulnerableTime,
                    interventionAppliedAtStart = policySnapshot.interventionActive,
                    debugSessionId = currentDebugSessionId ?: SessionManager.createDebugSessionId()
                )
                intentCheckedForCurrentSession = true
                sessionNeedsIntentCheck = false
                brakeSuppressedForCurrentSession = false
                scheduleBrakeForCurrentSession()
            },
            onCloseNowSelected = {
                intentCheckedForCurrentSession = true
                sessionNeedsIntentCheck = false
                brakeSuppressedForCurrentSession = true
            },
            onDismissed = {
                // Keep the intent check from reappearing after the user has made a choice.
            }
        )
    }

    private fun scheduleBrakeForCurrentSession() {
        cancelBrakeSchedule()

        if (brakeSuppressedForCurrentSession) return

        val session = SessionManager.getCurrentSession() ?: return
        val elapsedMillis = SessionManager.getElapsedMillis()
        val delayMillis = (session.currentLimitDurationMillis - elapsedMillis).coerceAtLeast(0L)

        Log.d(
            TAG,
            "brake scheduled package=${session.packageName} delay=$delayMillis currentLimit=${session.currentLimitDurationMillis}"
        )
        TeumLogger.session(
            debugSessionId = session.debugSessionId,
            event = "BRAKE_SCHEDULED",
            detail = "delay=$delayMillis"
        )
        brakeHandler.postDelayed(brakeRunnable, delayMillis)
    }

    private fun cancelBrakeSchedule() {
        brakeHandler.removeCallbacks(brakeRunnable)
    }

    private fun showSessionBrakeIfNeeded() {
        val session = SessionManager.getCurrentSession() ?: return
        if (brakeSuppressedForCurrentSession) return
        if (!SessionManager.hasActiveSessionFor(session.packageName)) return
        if (currentForegroundPackage != session.packageName) {
            Log.d(TAG, "brake skipped because foreground changed package=${session.packageName} foreground=$currentForegroundPackage")
            return
        }
        if (!SessionManager.isCurrentSessionOverrun()) {
            scheduleBrakeForCurrentSession()
            return
        }
        if (overlayController.overlayShowing) {
            Log.d(TAG, "brake skipped because overlay is already showing package=${session.packageName}")
            brakeHandler.postDelayed(brakeRunnable, BRAKE_RETRY_DELAY_MILLIS)
            return
        }

        overlayController.showSessionBrake(
            packageName = session.packageName,
            elapsedMillis = SessionManager.getElapsedMillis(),
            targetDurationMillis = session.currentLimitDurationMillis,
            interventionActive = session.interventionAppliedAtStart,
            extensionLimitReached = isExtensionLimitReached(session),
            debugSessionId = session.debugSessionId,
            source = "session_brake",
            onBrakeChoice = { choice ->
                handleBrakeChoice(choice, session.packageName)
            },
            onExtendDurationSelected = { durationMillis ->
                handleBrakeExtension(session.packageName, durationMillis)
            }
        )
        if (overlayController.currentOverlayName == "SESSION_BRAKE") {
            SessionManager.markInterventionShown()
        }
        val elapsedMillis = SessionManager.getElapsedMillis()
        TeumLogger.session(
            debugSessionId = session.debugSessionId,
            event = "BRAKE_SHOWN",
            detail = "elapsed=$elapsedMillis target=${session.targetDurationMillis} overrun=${elapsedMillis >= session.targetDurationMillis}"
        )
    }

    private fun handleBrakeChoice(choice: BrakeChoice, packageName: String) {
        when (choice) {
            BrakeChoice.END_NOW -> {
                val currentSession = SessionManager.getCurrentSession()
                val shouldShowOutcomeCheck = currentSession?.let(::shouldShowOutcomeCheckForEndNow) == true
                if (!shouldShowOutcomeCheck) {
                    SessionManager.markCurrentSessionOutcome(OutcomeType.ENDED)
                }

                val endedSession = SessionManager.endSession(
                    packageName = packageName,
                    reason = "end_now"
                )

                if (shouldShowOutcomeCheck && endedSession != null) {
                    brakeHandler.post {
                        showOutcomeCheckForClearPurpose(
                            endedSession = endedSession,
                            source = "session_brake_end_now",
                            saveReason = "session_brake_after_outcome"
                        )
                    }
                } else {
                    saveEndedSession(
                        session = endedSession,
                        reason = "end_now",
                        homeNavigationReason = "non_clear_end_now"
                    )
                }

                suppressReentry(
                    packageName = packageName,
                    reason = "after_end_now",
                    durationMillis = SUPPRESS_REENTRY_AFTER_END_MILLIS
                )
                brakeSuppressedForCurrentSession = true
                Log.d(TAG, "brake end selected package=$packageName")
            }

            BrakeChoice.EXTEND_3_MIN -> {
                handleBrakeExtension(packageName, THREE_MINUTES_MILLIS)
            }

            BrakeChoice.NECESSARY_USE -> {
                SessionManager.markInterventionHidden()
                SessionManager.markCurrentSessionOutcome(OutcomeType.NECESSARY_USE)
                brakeSuppressedForCurrentSession = true
                Log.d(TAG, "brake suppressed for necessary use package=$packageName")
            }

            BrakeChoice.PURPOSE_DRIFT -> {
                SessionManager.markInterventionHidden()
                SessionManager.markCurrentSessionOutcome(OutcomeType.PURPOSE_DRIFT)
                brakeSuppressedForCurrentSession = true
                Log.d(TAG, "brake suppressed for purpose drift package=$packageName")
            }
        }
    }

    private fun currentStablePackageName(): String? {
        return rootInActiveWindow?.packageName?.toString() ?: latestObservedPackage
    }

    private fun handleBrakeExtension(packageName: String, durationMillis: Long) {
        val session = SessionManager.getCurrentSession() ?: return
        val maxExtensions = maxExtensionCountForSession(session)
        TeumLogger.session(
            debugSessionId = session.debugSessionId,
            event = "EXTENSION_LIMIT_CHECK",
            detail = "mode=${session.modeAtStart} vulnerable=${session.isVulnerableTimeAtStart} " +
                "extensionCount=${session.extensionCount} max=$maxExtensions"
        )
        if (session.interventionAppliedAtStart && session.extensionCount >= maxExtensions) {
            TeumLogger.session(
                debugSessionId = session.debugSessionId,
                event = "EXTENSION_BLOCKED",
                detail = "reason=vulnerable_time_limit extensionCount=${session.extensionCount} max=$maxExtensions"
            )
            return
        }
        TeumLogger.session(
            debugSessionId = session.debugSessionId,
            event = "EXTENSION_ALLOWED",
            detail = "reason=${if (session.interventionAppliedAtStart) "within_vulnerable_time_limit" else "normal_mode"}"
        )
        SessionManager.markInterventionHidden()
        SessionManager.extendCurrentSession(durationMillis)
        brakeSuppressedForCurrentSession = false
        scheduleBrakeForCurrentSession()
        Log.d(TAG, "brake extended package=$packageName duration=$durationMillis")
    }

    private fun saveEndedSession(
        session: com.teum.app.session.AppSession?,
        reason: String = "unknown",
        homeNavigationReason: String? = null
    ) {
        if (session == null) return
        TeumLogger.session(session.debugSessionId, "DB_SAVE_REQUESTED", "reason=$reason")

        serviceScope.launch {
            try {
                val id = sessionLogRepository.saveEndedSession(session)
                if (id != null) {
                    val durationMillis = ((session.endedAtMillis ?: 0L) - session.startedAtMillis).coerceAtLeast(0L)
                    val overrun = durationMillis > session.targetDurationMillis
                    Log.d(
                        DB_TAG,
                        "session saved id=$id package=${session.packageName} duration=$durationMillis overrun=$overrun fastReopen=${session.isFastReopen}"
                    )
                    TeumLogger.session(session.debugSessionId, "DB_SAVED", "id=$id")
                    if (homeNavigationReason != null) {
                        requestHomeNavigation(
                            debugSessionId = session.debugSessionId,
                            reason = homeNavigationReason,
                            sessionLogId = id
                        )
                    }
                }
            } catch (exception: RuntimeException) {
                Log.e(DB_TAG, "failed to save session package=${session.packageName}", exception)
                if (homeNavigationReason != null) {
                    requestHomeNavigation(
                        debugSessionId = session.debugSessionId,
                        reason = homeNavigationReason,
                        sessionLogId = null
                    )
                }
            }
        }
    }

    private fun saveAppOpenEvent(
        packageName: String,
        detectedAtMillis: Long
    ) {
        serviceScope.launch {
            try {
                val id = sessionLogRepository.saveAppOpenEvent(
                    packageName = packageName,
                    detectedAtMillis = detectedAtMillis
                )
                Log.d(
                    DB_TAG,
                    "app open event saved id=$id package=$packageName detectedAt=$detectedAtMillis"
                )
            } catch (exception: RuntimeException) {
                Log.e(DB_TAG, "failed to save app open event package=$packageName", exception)
            }
        }
    }

    private fun handleTargetExitEndedSession(endedSession: AppSession) {
        if (endedSession.intentChoice != IntentChoice.CLEAR_PURPOSE) {
            TeumLogger.session(
                debugSessionId = endedSession.debugSessionId,
                event = "OUTCOME_SKIPPED",
                detail = "reason=intent_not_clear_purpose intent=${endedSession.intentChoice.name}"
            )
            saveEndedSession(
                session = endedSession,
                reason = "target_exit_without_outcome"
            )
            return
        }

        showOutcomeCheckForClearPurpose(
            endedSession = endedSession,
            source = "target_exit",
            saveReason = "target_exit_after_outcome"
        )
    }

    private fun showOutcomeCheckForClearPurpose(
        endedSession: AppSession,
        source: String,
        saveReason: String
    ) {
        TeumLogger.session(
            debugSessionId = endedSession.debugSessionId,
            event = "OUTCOME_PREINSERT_REQUESTED",
            detail = "source=$source reason=$saveReason"
        )

        serviceScope.launch {
            try {
                val sessionLogId = sessionLogRepository.saveEndedSession(endedSession)
                if (sessionLogId == null) {
                    Log.e(DB_TAG, "failed to preinsert outcome session package=${endedSession.packageName}")
                    return@launch
                }

                TeumLogger.session(
                    debugSessionId = endedSession.debugSessionId,
                    event = "OUTCOME_PREINSERT_SAVED",
                    detail = "sessionLogId=$sessionLogId"
                )
                Log.d(
                    DB_TAG,
                    "outcome preinsert saved id=$sessionLogId package=${endedSession.packageName} source=$source"
                )

                brakeHandler.post {
                    pendingOutcome = PendingOutcome(
                        session = endedSession,
                        sessionLogId = sessionLogId,
                        homeNavigationReason = if (source == "session_brake_end_now") {
                            "session_brake_end_now_after_outcome"
                        } else {
                            null
                        }
                    )
                    showOutcomeCheckOverlay(
                        endedSession = endedSession,
                        source = source
                    )
                }
            } catch (exception: RuntimeException) {
                Log.e(DB_TAG, "failed to preinsert outcome session package=${endedSession.packageName}", exception)
            }
        }
    }

    private fun showOutcomeCheckOverlay(
        endedSession: AppSession,
        source: String
    ) {
        overlayController.showOutcomeCheck(
            packageName = endedSession.packageName,
            debugSessionId = endedSession.debugSessionId,
            durationMillis = getSessionDurationMillis(endedSession),
            intentChoice = endedSession.intentChoice,
            targetDurationMillis = endedSession.targetDurationMillis +
                endedSession.totalExtensionDurationMillis,
            extensionCount = endedSession.extensionCount,
            interventionActive = endedSession.interventionAppliedAtStart,
            source = source,
            onOutcomeSelected = { outcomeType ->
                handleOutcomeSelected(outcomeType)
            },
            onDismissedWithoutChoice = {
                handleOutcomeDismissed()
            }
        )
    }

    private fun handleOutcomeSelected(outcomeType: OutcomeType) {
        val pending = pendingOutcome ?: return
        if (pending.handled) {
            TeumLogger.session(
                debugSessionId = pending.session.debugSessionId,
                event = "OUTCOME_IGNORED_ALREADY_HANDLED",
                detail = "sessionLogId=${pending.sessionLogId} event=selected"
            )
            return
        }

        pending.handled = true
        pendingOutcome = null
        TeumLogger.session(
            debugSessionId = pending.session.debugSessionId,
            event = "OUTCOME_SELECTED",
            detail = "outcome=$outcomeType sessionLogId=${pending.sessionLogId}"
        )
        updatePendingOutcome(
            pending = pending,
            outcomeType = outcomeType,
            dismissed = false
        )
    }

    private fun handleOutcomeDismissed() {
        val pending = pendingOutcome ?: return
        if (pending.handled) {
            TeumLogger.session(
                debugSessionId = pending.session.debugSessionId,
                event = "OUTCOME_IGNORED_ALREADY_HANDLED",
                detail = "sessionLogId=${pending.sessionLogId} event=dismissed"
            )
            return
        }

        pending.handled = true
        pendingOutcome = null
        TeumLogger.session(
            debugSessionId = pending.session.debugSessionId,
            event = "OUTCOME_DISMISSED",
            detail = "sessionLogId=${pending.sessionLogId}"
        )
        TeumLogger.session(
            debugSessionId = pending.session.debugSessionId,
            event = "OUTCOME_DISMISS_LEFT_UNANSWERED",
            detail = "sessionLogId=${pending.sessionLogId}"
        )
        pending.homeNavigationReason?.let { reason ->
            requestHomeNavigation(
                debugSessionId = pending.session.debugSessionId,
                reason = reason,
                sessionLogId = pending.sessionLogId
            )
        }
    }

    private fun updatePendingOutcome(
        pending: PendingOutcome,
        outcomeType: OutcomeType,
        dismissed: Boolean
    ) {
        TeumLogger.session(
            debugSessionId = pending.session.debugSessionId,
            event = "OUTCOME_UPDATE_REQUESTED",
            detail = "sessionLogId=${pending.sessionLogId} outcome=$outcomeType dismissed=$dismissed"
        )

        serviceScope.launch {
            try {
                val success = sessionLogRepository.updateSessionOutcome(
                    sessionId = pending.sessionLogId,
                    outcomeType = outcomeType
                )
                TeumLogger.session(
                    debugSessionId = pending.session.debugSessionId,
                    event = "OUTCOME_UPDATED",
                    detail = "sessionLogId=${pending.sessionLogId} success=$success"
                )
                pending.homeNavigationReason?.let { reason ->
                    requestHomeNavigation(
                        debugSessionId = pending.session.debugSessionId,
                        reason = reason,
                        sessionLogId = pending.sessionLogId
                    )
                }
            } catch (exception: RuntimeException) {
                Log.e(DB_TAG, "failed to update outcome sessionLogId=${pending.sessionLogId}", exception)
                pending.homeNavigationReason?.let { reason ->
                    requestHomeNavigation(
                        debugSessionId = pending.session.debugSessionId,
                        reason = reason,
                        sessionLogId = pending.sessionLogId
                    )
                }
            }
        }
    }

    private fun requestHomeNavigation(
        debugSessionId: Long,
        reason: String,
        sessionLogId: Long?
    ) {
        cancelPendingTargetEnter(reason = "home_navigation", currentPackage = latestObservedPackage)
        ignoreTargetEnterUntilMillis = System.currentTimeMillis() + HOME_NAVIGATION_ENTER_GUARD_MILLIS
        val sessionLogDetail = sessionLogId?.let { " sessionLogId=$it" }.orEmpty()
        TeumLogger.session(
            debugSessionId = debugSessionId,
            event = "HOME_NAVIGATION_REQUESTED",
            detail = "reason=$reason$sessionLogDetail"
        )
        brakeHandler.post {
            val success = performGlobalAction(GLOBAL_ACTION_HOME)
            if (success) {
                ignoreTargetEnterUntilMillis =
                    System.currentTimeMillis() + HOME_NAVIGATION_ENTER_GUARD_MILLIS
            }
            TeumLogger.session(
                debugSessionId = debugSessionId,
                event = "HOME_NAVIGATION_RESULT",
                detail = "success=$success reason=$reason"
            )
            if (success && sessionLogId != null) {
                confirmExitAfterIntervention(
                    debugSessionId = debugSessionId,
                    sessionLogId = sessionLogId,
                    reason = "home_navigation_success"
                )
            } else if (!success && sessionLogId != null) {
                TeumLogger.session(
                    debugSessionId = debugSessionId,
                    event = "EXIT_CONFIRM_SKIPPED",
                    detail = "reason=home_navigation_failed sessionLogId=$sessionLogId"
                )
            }
        }
    }

    private fun confirmExitAfterIntervention(
        debugSessionId: Long,
        sessionLogId: Long,
        reason: String
    ) {
        TeumLogger.session(
            debugSessionId = debugSessionId,
            event = "EXIT_CONFIRM_REQUESTED",
            detail = "sessionLogId=$sessionLogId reason=$reason"
        )
        serviceScope.launch {
            try {
                val success = sessionLogRepository.confirmExitAfterIntervention(sessionLogId)
                TeumLogger.session(
                    debugSessionId = debugSessionId,
                    event = "EXIT_CONFIRMED",
                    detail = "sessionLogId=$sessionLogId success=$success"
                )
            } catch (exception: RuntimeException) {
                Log.e(DB_TAG, "failed to confirm exit sessionLogId=$sessionLogId", exception)
            }
        }
    }

    private fun shouldShowOutcomeCheckForTargetExit(session: AppSession): Boolean {
        return session.intentChoice == IntentChoice.CLEAR_PURPOSE
    }

    private fun shouldShowOutcomeCheckForEndNow(session: AppSession): Boolean {
        return session.intentChoice == IntentChoice.CLEAR_PURPOSE
    }

    private suspend fun resolvePolicySnapshot(nowMillis: Long): PolicySnapshot {
        val mode = userSettingsRepository.getInterventionMode()
        val isVulnerableTime = try {
            vulnerableTimeRepository.isVulnerableNow(nowMillis = nowMillis)
        } catch (exception: RuntimeException) {
            Log.w(TAG, "failed to resolve vulnerable time policy", exception)
            false
        }
        val interventionActive = mode.isIntervention && isVulnerableTime
        val snapshot = PolicySnapshot(
            mode = mode,
            isVulnerableTime = isVulnerableTime,
            interventionActive = interventionActive
        )
        val durations = availableDurationsForPolicy(snapshot)

        TeumLogger.flow(
            "[POLICY] MODE_RESOLVED mode=${mode.name} vulnerable=$isVulnerableTime " +
                "interventionActive=$interventionActive"
        )
        TeumLogger.flow(
            "[POLICY] AVAILABLE_DURATIONS durations=${durations.joinToString(",") { it.name }} " +
                "reason=${if (interventionActive) "vulnerable_time_intervention_mode" else "normal_or_not_vulnerable"}"
        )
        if (interventionActive) {
            TeumLogger.flow(
                "[POLICY] DURATION_LIMIT_APPLIED max=${TargetDurationChoice.FIVE_MINUTES.name} " +
                    "reason=vulnerable_time_intervention_mode"
            )
        }

        return snapshot
    }

    private fun availableDurationsForPolicy(policySnapshot: PolicySnapshot): List<TargetDurationChoice> {
        return if (policySnapshot.interventionActive) {
            listOf(
                TargetDurationChoice.TEST_FIVE_SECONDS,
                TargetDurationChoice.ONE_MINUTE,
                TargetDurationChoice.THREE_MINUTES,
                TargetDurationChoice.FIVE_MINUTES
            )
        } else {
            TargetDurationChoice.entries.toList()
        }
    }

    private fun maxExtensionCountForSession(session: AppSession): Int {
        return if (session.interventionAppliedAtStart) 1 else Int.MAX_VALUE
    }

    private fun isExtensionLimitReached(session: AppSession): Boolean {
        return session.interventionAppliedAtStart &&
            session.extensionCount >= maxExtensionCountForSession(session)
    }

    private fun suppressReentry(
        packageName: String,
        reason: String,
        durationMillis: Long
    ) {
        suppressReentryUntilByPackage[packageName] =
            System.currentTimeMillis() + durationMillis
        suppressReentryReasonByPackage[packageName] = reason
        TeumLogger.flow(
            "[ACCESS] REENTRY_SUPPRESS_SET package=$packageName reason=$reason duration=$durationMillis"
        )
    }

    private fun shouldSuppressReentry(packageName: String): Boolean {
        val nowMillis = System.currentTimeMillis()
        val suppressUntilMillis = suppressReentryUntilByPackage[packageName] ?: return false

        if (nowMillis < suppressUntilMillis) {
            val remainingMillis = suppressUntilMillis - nowMillis
            val reason = suppressReentryReasonByPackage[packageName] ?: "unknown"
            TeumLogger.flow(
                "[ACCESS] ENTER_SUPPRESSED package=$packageName reason=$reason remainingMs=$remainingMillis"
            )
            return true
        }

        suppressReentryUntilByPackage.remove(packageName)
        suppressReentryReasonByPackage.remove(packageName)
        TeumLogger.flow("[ACCESS] REENTRY_SUPPRESS_EXPIRED package=$packageName")
        return false
    }

    private fun getSessionDurationMillis(session: AppSession): Long {
        val endedAtMillis = session.endedAtMillis ?: System.currentTimeMillis()
        return (endedAtMillis - session.startedAtMillis).coerceAtLeast(0L)
    }

    private fun resetIntentCheckSession() {
        activeTargetPackage = null
        currentEntryTimeMillis = null
        currentReopenCheckResult = null
        currentDebugSessionId = null
        currentPolicySnapshot = PolicySnapshot()
        sessionNeedsIntentCheck = false
        intentCheckedForCurrentSession = false
        brakeSuppressedForCurrentSession = false
        if (overlayController.currentOverlayName == "OUTCOME_CHECK") {
            TeumLogger.overlay(
                event = "RESET_SKIPPED",
                detail = "reason=outcome_pending overlay=OUTCOME_CHECK"
            )
            return
        }
        overlayController.dismiss()
    }

    private fun ownPackageName(): String = applicationContext.packageName

    private data class PendingOutcome(
        val session: AppSession,
        val sessionLogId: Long,
        val homeNavigationReason: String? = null,
        var handled: Boolean = false
    )

    private data class PolicySnapshot(
        val mode: InterventionMode = InterventionMode.NORMAL,
        val isVulnerableTime: Boolean = false,
        val interventionActive: Boolean = false
    )

    private companion object {
        const val TAG = "TeumAccess"
        const val DB_TAG = "TeumDB"
        const val THREE_MINUTES_MILLIS = 180_000L
        const val BRAKE_RETRY_DELAY_MILLIS = 1_000L
        const val SUPPRESS_REENTRY_AFTER_END_MILLIS = 10_000L
        const val SUPPRESS_REENTRY_AFTER_TARGET_EXIT_MILLIS = 1_500L
        const val ENTER_CONFIRM_DELAY_MILLIS = 400L
        const val HOME_NAVIGATION_ENTER_GUARD_MILLIS = 2_000L
    }
}
