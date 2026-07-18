package com.teum.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.teum.app.data.repository.SessionLogRepository
import com.teum.app.data.repository.TargetAppRepository
import com.teum.app.overlay.BrakeChoice
import com.teum.app.overlay.IntentCheckMode
import com.teum.app.overlay.OverlayController
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
            cancelBrakeSchedule()
            if (SessionManager.hasActiveSessionFor(previousPackage)) {
                saveEndedSession(SessionManager.endSession(previousPackage))
            }
            resetIntentCheckSession()
        }

        if (targetAppRepository.isTargetPackage(packageName)) {
            Log.d(TAG, "target app entered: $packageName")
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
        if (overlayController.overlayShowing) return

        overlayController.showIntentCheck(
            packageName = packageName,
            mode = if (currentReopenCheckResult?.isFastReopen == true) {
                IntentCheckMode.FAST_REOPEN
            } else {
                IntentCheckMode.NORMAL
            },
            reopenGapMillis = currentReopenCheckResult?.gapMillis,
            onIntentConfirmed = { intentChoice, targetDurationMillis ->
                val entryTimeMillis = currentEntryTimeMillis ?: System.currentTimeMillis()
                val reopenCheckResult = currentReopenCheckResult
                SessionManager.startSession(
                    packageName = packageName,
                    intentChoice = intentChoice,
                    targetDurationMillis = targetDurationMillis,
                    entryDetectedAtMillis = entryTimeMillis,
                    isFastReopen = reopenCheckResult?.isFastReopen == true,
                    reopenGapMillis = reopenCheckResult?.gapMillis
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
            onBrakeChoice = { choice ->
                handleBrakeChoice(choice, session.packageName)
            }
        )
    }

    private fun handleBrakeChoice(choice: BrakeChoice, packageName: String) {
        when (choice) {
            BrakeChoice.END_NOW -> {
                SessionManager.markCurrentSessionOutcome(OutcomeType.ENDED)
                saveEndedSession(SessionManager.endSession(packageName))
                brakeSuppressedForCurrentSession = true
                Log.d(TAG, "brake end selected package=$packageName")
            }

            BrakeChoice.EXTEND_3_MIN -> {
                SessionManager.extendCurrentSession(THREE_MINUTES_MILLIS)
                brakeSuppressedForCurrentSession = false
                scheduleBrakeForCurrentSession()
            }

            BrakeChoice.NECESSARY_USE -> {
                SessionManager.markCurrentSessionOutcome(OutcomeType.NECESSARY_USE)
                brakeSuppressedForCurrentSession = true
                Log.d(TAG, "brake suppressed for necessary use package=$packageName")
            }

            BrakeChoice.PURPOSE_DRIFT -> {
                SessionManager.markCurrentSessionOutcome(OutcomeType.PURPOSE_DRIFT)
                brakeSuppressedForCurrentSession = true
                Log.d(TAG, "brake suppressed for purpose drift package=$packageName")
            }
        }
    }

    private fun saveEndedSession(session: com.teum.app.session.AppSession?) {
        if (session == null) return

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

    private fun resetIntentCheckSession() {
        activeTargetPackage = null
        currentEntryTimeMillis = null
        currentReopenCheckResult = null
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
    }
}
