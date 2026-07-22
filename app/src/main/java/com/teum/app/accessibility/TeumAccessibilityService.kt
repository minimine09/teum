package com.teum.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.teum.app.debug.TeumLogger
import com.teum.app.data.repository.SessionLogRepository
import com.teum.app.data.repository.TargetAppRepository
import com.teum.app.overlay.BrakeChoice
import com.teum.app.overlay.IntentChoice
import com.teum.app.overlay.IntentCheckMode
import com.teum.app.overlay.OverlayController
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
    private var pendingOutcomeSession: AppSession? = null
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
        serviceScope.cancel()
        overlayController.removeOverlayIfAttached()
        super.onDestroy()
    }

    private fun handleForegroundPackageChanged(packageName: String) {
        val previousPackage = currentForegroundPackage
        currentForegroundPackage = packageName

        if (previousPackage != null && targetAppRepository.isTargetPackage(previousPackage)) {
            Log.d(TAG, "target app exited: $previousPackage")
            TeumLogger.access("EXIT", previousPackage)
            cancelBrakeSchedule()
            var endedSession: AppSession? = null
            if (SessionManager.hasActiveSessionFor(previousPackage)) {
                if (overlayController.currentOverlayName == "SESSION_BRAKE") {
                    SessionManager.markInterventionHidden()
                }
                endedSession = SessionManager.endSession(
                    packageName = previousPackage,
                    reason = "target_exit"
                )
                if (endedSession != null) {
                    suppressReentry(
                        packageName = previousPackage,
                        reason = "after_target_exit",
                        durationMillis = SUPPRESS_REENTRY_AFTER_TARGET_EXIT_MILLIS
                    )
                }
            }
            resetIntentCheckSession()
            endedSession?.let(::handleTargetExitEndedSession)
        }

        if (targetAppRepository.isTargetPackage(packageName)) {
            if (shouldSuppressReentry(packageName)) return

            Log.d(TAG, "target app entered: $packageName")
            TeumLogger.access("ENTER", packageName)
            val entryTimeMillis = System.currentTimeMillis()
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
        } else {
            resetIntentCheckSession()
        }
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
            debugSessionId = currentDebugSessionId,
            source = intentCheckSource,
            onIntentConfirmed = { intentChoice, targetDurationMillis ->
                val entryTimeMillis = currentEntryTimeMillis ?: System.currentTimeMillis()
                val reopenCheckResult = currentReopenCheckResult
                SessionManager.startSession(
                    packageName = packageName,
                    intentChoice = intentChoice,
                    targetDurationMillis = targetDurationMillis,
                    entryDetectedAtMillis = entryTimeMillis,
                    isFastReopen = reopenCheckResult?.isFastReopen == true,
                    reopenGapMillis = reopenCheckResult?.gapMillis,
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
                        reason = "end_now"
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

    private fun handleBrakeExtension(packageName: String, durationMillis: Long) {
        SessionManager.markInterventionHidden()
        SessionManager.extendCurrentSession(durationMillis)
        brakeSuppressedForCurrentSession = false
        scheduleBrakeForCurrentSession()
        Log.d(TAG, "brake extended package=$packageName duration=$durationMillis")
    }

    private fun saveEndedSession(
        session: com.teum.app.session.AppSession?,
        reason: String = "unknown"
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
                }
            } catch (exception: RuntimeException) {
                Log.e(DB_TAG, "failed to save session package=${session.packageName}", exception)
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
        val durationMillis = getSessionDurationMillis(endedSession)
        val shouldShowOutcomeCheck = shouldShowOutcomeCheckForTargetExit(endedSession)
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

        if (!shouldShowOutcomeCheck) {
            TeumLogger.session(
                debugSessionId = endedSession.debugSessionId,
                event = "OUTCOME_SKIPPED",
                detail = "reason=duration_too_short duration=$durationMillis"
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
        pendingOutcomeSession = endedSession
        overlayController.showOutcomeCheck(
            packageName = endedSession.packageName,
            debugSessionId = endedSession.debugSessionId,
            durationMillis = getSessionDurationMillis(endedSession),
            intentChoice = endedSession.intentChoice,
            source = source,
            onOutcomeSelected = { outcomeType ->
                TeumLogger.session(
                    debugSessionId = endedSession.debugSessionId,
                    event = "OUTCOME_SELECTED",
                    detail = "outcome=$outcomeType"
                )
                saveEndedSession(
                    session = endedSession.copy(outcomeType = outcomeType),
                    reason = saveReason
                )
                pendingOutcomeSession = null
            },
            onDismissedWithoutChoice = {
                TeumLogger.session(
                    debugSessionId = endedSession.debugSessionId,
                    event = "OUTCOME_DISMISSED"
                )
                saveEndedSession(
                    session = endedSession,
                    reason = "${saveReason}_dismissed"
                )
                pendingOutcomeSession = null
            }
        )
    }

    private fun shouldShowOutcomeCheckForTargetExit(session: AppSession): Boolean {
        return session.intentChoice == IntentChoice.CLEAR_PURPOSE &&
            getSessionDurationMillis(session) >= MIN_DURATION_FOR_OUTCOME_CHECK_MILLIS
    }

    private fun shouldShowOutcomeCheckForEndNow(session: AppSession): Boolean {
        return session.intentChoice == IntentChoice.CLEAR_PURPOSE
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
        sessionNeedsIntentCheck = false
        intentCheckedForCurrentSession = false
        brakeSuppressedForCurrentSession = false
        overlayController.dismiss()
    }

    private fun ownPackageName(): String = applicationContext.packageName

    private companion object {
        const val TAG = "TeumAccess"
        const val DB_TAG = "TeumDB"
        const val THREE_MINUTES_MILLIS = 180_000L
        const val BRAKE_RETRY_DELAY_MILLIS = 1_000L
        const val SUPPRESS_REENTRY_AFTER_END_MILLIS = 10_000L
        const val SUPPRESS_REENTRY_AFTER_TARGET_EXIT_MILLIS = 1_500L
        // Demo value. For production UX, consider 10_000L.
        const val MIN_DURATION_FOR_OUTCOME_CHECK_MILLIS = 3_000L
    }
}
