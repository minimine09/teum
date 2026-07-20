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

    fun endSession(packageName: String): AppSession? {
        val session = state.currentSession ?: return null
        if (session.packageName != packageName) return null

        val endedAtMillis = System.currentTimeMillis()
        val durationMillis = endedAtMillis - session.startedAtMillis
        val overrun = durationMillis > session.targetDurationMillis

        Log.d(
            TAG,
            "session ended package=$packageName duration=$durationMillis overrun=$overrun outcome=${session.outcomeType}"
        )
        TeumLogger.session(
            debugSessionId = session.debugSessionId,
            event = "END",
            detail = "duration=$durationMillis overrun=$overrun outcome=${session.outcomeType}"
        )

        val endedSession = session.copy(endedAtMillis = endedAtMillis)
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

    fun extendCurrentSession(extraMillis: Long) {
        val session = state.currentSession ?: return
        val updatedSession = session.copy(
            currentLimitDurationMillis = session.currentLimitDurationMillis + extraMillis,
            extensionCount = session.extensionCount + 1,
            outcomeType = OutcomeType.EXTENDED
        )
        state = SessionState(currentSession = updatedSession)

        Log.d(
            TAG,
            "session extended package=${updatedSession.packageName} extensionCount=${updatedSession.extensionCount} " +
                "originalTarget=${updatedSession.targetDurationMillis} currentLimit=${updatedSession.currentLimitDurationMillis}"
        )
        TeumLogger.session(
            debugSessionId = updatedSession.debugSessionId,
            event = "EXTEND",
            detail = "extensionCount=${updatedSession.extensionCount} newTarget=${updatedSession.targetDurationMillis}"
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

    fun clearCurrentSession() {
        val session = state.currentSession
        if (session != null) {
            Log.d(TAG, "session cleared package=${session.packageName}")
        }
        state = SessionState()
    }

    private const val DEFAULT_REOPEN_THRESHOLD_MILLIS = 5 * 60 * 1_000L
}
