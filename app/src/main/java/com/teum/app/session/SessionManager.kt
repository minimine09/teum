package com.teum.app.session

import android.util.Log
import com.teum.app.debug.TeumLogger
import com.teum.app.overlay.IntentChoice

object SessionManager {
    private const val TAG = "TeumSession"
    private const val REOPEN_TAG = "TeumReopen"

    private var state = SessionState()
    private var nextDebugSessionId = 1L
    private val lastEndedSessionByPackage = mutableMapOf<String, AppSession>()

    fun createDebugSessionId(): Long {
        return nextDebugSessionId++
    }

    fun startSession(
        packageName: String,
        intentChoice: IntentChoice,
        targetDurationMillis: Long,
        entryDetectedAtMillis: Long = System.currentTimeMillis(),
        isFastReopen: Boolean = false,
        reopenGapMillis: Long? = null,
        debugSessionId: Long = createDebugSessionId()
    ) {
        val session = AppSession(
            debugSessionId = debugSessionId,
            packageName = packageName,
            entryDetectedAtMillis = entryDetectedAtMillis,
            startedAtMillis = System.currentTimeMillis(),
            intentChoice = intentChoice,
            targetDurationMillis = targetDurationMillis,
            isFastReopen = isFastReopen,
            reopenGapMillis = reopenGapMillis
        )

        state = SessionState(currentSession = session)

        Log.d(
            TAG,
            "session started package=$packageName intent=${intentChoice.name} target=$targetDurationMillis fastReopen=$isFastReopen gap=$reopenGapMillis"
        )
        TeumLogger.session(
            debugSessionId = session.debugSessionId,
            event = "START",
            detail = "package=$packageName intent=${intentChoice.name} target=$targetDurationMillis fastReopen=$isFastReopen gap=$reopenGapMillis"
        )
    }

    fun checkFastReopen(
        packageName: String,
        currentEntryTimeMillis: Long,
        thresholdMillis: Long = DEFAULT_REOPEN_THRESHOLD_MILLIS
    ): ReopenCheckResult {
        val previousSession = lastEndedSessionByPackage[packageName]
        val previousEndTimeMillis = previousSession?.endedAtMillis

        if (previousEndTimeMillis == null) {
            Log.d(REOPEN_TAG, "normal entry package=$packageName no previous session")
            TeumLogger.reopen("NORMAL package=$packageName reason=no_previous_session")
            return ReopenCheckResult(
                isFastReopen = false,
                gapMillis = null,
                previousEndTimeMillis = null
            )
        }

        val gapMillis = (currentEntryTimeMillis - previousEndTimeMillis).coerceAtLeast(0L)
        val isFastReopen = gapMillis <= thresholdMillis

        if (isFastReopen) {
            Log.d(REOPEN_TAG, "fast reopen detected package=$packageName gap=$gapMillis threshold=$thresholdMillis")
            TeumLogger.reopen("FAST package=$packageName gap=$gapMillis threshold=$thresholdMillis")
        } else {
            Log.d(REOPEN_TAG, "normal entry package=$packageName gap=$gapMillis threshold=$thresholdMillis")
            TeumLogger.reopen("NORMAL package=$packageName gap=$gapMillis threshold=$thresholdMillis")
        }

        return ReopenCheckResult(
            isFastReopen = isFastReopen,
            gapMillis = gapMillis,
            previousEndTimeMillis = previousEndTimeMillis
        )
    }

    fun endSession(packageName: String, reason: String = "unknown"): AppSession? {
        val session = state.currentSession ?: return null
        if (session.packageName != packageName) return null
        closeCurrentInterventionIfNeeded()

        val updatedSession = state.currentSession ?: return null
        val endedAtMillis = System.currentTimeMillis()
        val durationMillis = endedAtMillis - updatedSession.startedAtMillis
        val overrun = durationMillis > updatedSession.targetDurationMillis

        Log.d(
            TAG,
            "session ended package=$packageName duration=$durationMillis overrun=$overrun outcome=${session.outcomeType}"
        )
        TeumLogger.session(
            debugSessionId = updatedSession.debugSessionId,
            event = "END",
            detail = "reason=$reason duration=$durationMillis overrun=$overrun outcome=${updatedSession.outcomeType}"
        )

        val endedSession = updatedSession.copy(endedAtMillis = endedAtMillis)
        lastEndedSessionByPackage[packageName] = endedSession
        state = SessionState(currentSession = endedSession)
        state = SessionState()
        return endedSession
    }

    fun getCurrentSession(): AppSession? {
        return state.currentSession
    }

    fun hasActiveSessionFor(packageName: String): Boolean {
        return state.currentSession?.packageName == packageName
    }

    fun isCurrentSessionOverrun(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val session = state.currentSession ?: return false
        return getElapsedMillis(nowMillis) >= session.currentLimitDurationMillis
    }

    fun getElapsedMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val session = state.currentSession ?: return 0L
        return (nowMillis - session.startedAtMillis).coerceAtLeast(0L)
    }

    fun extendCurrentSession(extraMillis: Long, nowMillis: Long = System.currentTimeMillis()) {
        val session = state.currentSession ?: return
        val elapsedMillis = getElapsedMillis(nowMillis)
        val nextLimitDurationMillis = elapsedMillis + extraMillis
        val updatedSession = session.copy(
            currentLimitDurationMillis = nextLimitDurationMillis,
            extensionCount = session.extensionCount + 1,
            totalExtensionDurationMillis = session.totalExtensionDurationMillis + extraMillis,
            outcomeType = OutcomeType.EXTENDED
        )
        state = SessionState(currentSession = updatedSession)

        Log.d(
            TAG,
            "session extended package=${updatedSession.packageName} extensionCount=${updatedSession.extensionCount} " +
                "originalTarget=${updatedSession.targetDurationMillis} currentLimit=${updatedSession.currentLimitDurationMillis} " +
                "nextBrakeDelay=$extraMillis"
        )
        TeumLogger.session(
            debugSessionId = updatedSession.debugSessionId,
            event = "EXTEND",
            detail = "duration=$extraMillis extensionCount=${updatedSession.extensionCount} " +
                "nextBrakeDelay=$extraMillis nextLimit=${updatedSession.currentLimitDurationMillis}"
        )
    }

    fun markCurrentSessionOutcome(outcomeType: OutcomeType) {
        val session = state.currentSession ?: return
        state = SessionState(currentSession = session.copy(outcomeType = outcomeType))

        Log.d(
            TAG,
            "session outcome marked package=${session.packageName} outcome=$outcomeType"
        )
        TeumLogger.session(
            debugSessionId = session.debugSessionId,
            event = "OUTCOME",
            detail = "outcome=$outcomeType"
        )
    }

    fun markInterventionShown(nowMillis: Long = System.currentTimeMillis()) {
        val session = state.currentSession ?: return
        if (session.currentInterventionStartedAtMillis != null) return

        state = SessionState(
            currentSession = session.copy(currentInterventionStartedAtMillis = nowMillis)
        )
        TeumLogger.session(
            debugSessionId = session.debugSessionId,
            event = "INTERVENTION_SHOWN",
            detail = "type=SESSION_BRAKE at=$nowMillis"
        )
    }

    fun markInterventionHidden(nowMillis: Long = System.currentTimeMillis()) {
        closeCurrentInterventionIfNeeded(nowMillis)
    }

    fun clearCurrentSession() {
        val session = state.currentSession
        if (session != null) {
            Log.d(TAG, "session cleared package=${session.packageName}")
        }
        state = SessionState()
    }

    private fun closeCurrentInterventionIfNeeded(nowMillis: Long = System.currentTimeMillis()) {
        val session = state.currentSession ?: return
        val startedAtMillis = session.currentInterventionStartedAtMillis ?: return
        val visibleMillis = (nowMillis - startedAtMillis).coerceAtLeast(0L)
        val updatedSession = session.copy(
            interventionVisibleMillis = session.interventionVisibleMillis + visibleMillis,
            currentInterventionStartedAtMillis = null
        )
        state = SessionState(currentSession = updatedSession)
        TeumLogger.session(
            debugSessionId = updatedSession.debugSessionId,
            event = "INTERVENTION_ACCUMULATED",
            detail = "type=SESSION_BRAKE visible=$visibleMillis total=${updatedSession.interventionVisibleMillis}"
        )
    }

    private const val DEFAULT_REOPEN_THRESHOLD_MILLIS = 5 * 60 * 1_000L
}
