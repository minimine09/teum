package com.teum.app.data.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.room.withTransaction
import com.teum.app.data.local.TeumDatabase
import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.ExtensionEventEntity
import com.teum.app.data.local.entity.ReopenLogEntity
import com.teum.app.data.local.entity.SelfControlEventEntity
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.overlay.IntentChoice
import com.teum.app.session.AppSession
import com.teum.app.session.OutcomeType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SessionLogRepository(context: Context) {
    private val applicationContext = context.applicationContext
    private val packageManager = applicationContext.packageManager
    private val database = TeumDatabase.getInstance(applicationContext)
    private val sessionLogDao = database.sessionLogDao()
    private val appOpenEventDao = database.appOpenEventDao()
    private val extensionEventDao = database.extensionEventDao()
    private val reopenLogDao = database.reopenLogDao()
    private val selfControlEventDao = database.selfControlEventDao()

    suspend fun saveAppOpenEvent(
        packageName: String,
        detectedAtMillis: Long
    ): Long {
        return appOpenEventDao.insertAppOpenEvent(
            AppOpenEventEntity(
                packageName = packageName,
                detectedAtMillis = detectedAtMillis
            )
        )
    }

    suspend fun saveExtensionEvent(
        sessionId: Long,
        occurredAtMillis: Long,
        extensionDurationMillis: Long,
        interventionActiveAtTime: Boolean
    ): Long {
        return extensionEventDao.insertExtensionEvent(
            ExtensionEventEntity(
                sessionId = sessionId,
                occurredAtMillis = occurredAtMillis,
                extensionDurationMillis = extensionDurationMillis,
                interventionActiveAtTime = interventionActiveAtTime
            )
        )
    }

    fun observeExtensionEventsSince(
        sinceMillis: Long
    ): Flow<List<ExtensionEventEntity>> {
        return extensionEventDao.observeExtensionEventsSince(sinceMillis)
    }

    fun observeExtensionEventsForSession(
        sessionId: Long
    ): Flow<List<ExtensionEventEntity>> {
        return extensionEventDao.observeExtensionEventsForSession(sessionId)
    }

    suspend fun saveCloseNowBeforeSessionEvent(
        packageName: String,
        occurredAtMillis: Long = System.currentTimeMillis(),
        modeAtTime: String?,
        isVulnerableTimeAtTime: Boolean,
        interventionActiveAtTime: Boolean,
        source: String?
    ): Long {
        return selfControlEventDao.insertSelfControlEvent(
            SelfControlEventEntity(
                packageName = packageName,
                appDisplayName = resolveAppDisplayName(packageName),
                eventType = EVENT_CLOSE_NOW_BEFORE_SESSION,
                occurredAtMillis = occurredAtMillis,
                modeAtTime = modeAtTime,
                isVulnerableTimeAtTime = isVulnerableTimeAtTime,
                interventionActiveAtTime = interventionActiveAtTime,
                source = source
            )
        )
    }

    suspend fun saveEndedSession(session: AppSession): Long? {
        val endedAtMillis = session.endedAtMillis ?: return null
        val savedAtMillis = System.currentTimeMillis()
        val reopenCheck = checkReopen(
            packageName = session.packageName,
            currentEntryTimeMillis = session.entryDetectedAtMillis
        )
        val durationMillis = (endedAtMillis - session.startedAtMillis).coerceAtLeast(0L)
        val interventionVisibleMillis = session.interventionVisibleMillis.coerceAtLeast(0L)
        val effectiveUsageMillis = (durationMillis - interventionVisibleMillis).coerceAtLeast(0L)
        val totalExtensionDurationMillis = session.totalExtensionDurationMillis.coerceAtLeast(0L)
        val finalTargetDurationMillis =
            (session.targetDurationMillis + totalExtensionDurationMillis).coerceAtLeast(0L)
        val rawOverrunMillis =
            (effectiveUsageMillis - finalTargetDurationMillis).coerceAtLeast(0L)
        val isNecessaryUse =
            session.intentChoice == IntentChoice.CLEAR_PURPOSE &&
                session.outcomeType == OutcomeType.NECESSARY_USE
        val overrunMillis = rawOverrunMillis
        val necessaryUseExcessMillis =
            if (isNecessaryUse) rawOverrunMillis else 0L
        val overrunDetectedAtMillis = if (rawOverrunMillis > 0L) {
            session.overrunDetectedAtMillis ?: (endedAtMillis - rawOverrunMillis)
        } else {
            null
        }

        com.teum.app.debug.TeumLogger.session(
            debugSessionId = session.debugSessionId,
            event = "DB_SAVE_VALUES",
            detail = "duration=$durationMillis intervention=$interventionVisibleMillis " +
                "effective=$effectiveUsageMillis initialTarget=${session.targetDurationMillis} " +
                "extensionTotal=$totalExtensionDurationMillis finalTarget=$finalTargetDurationMillis " +
                "rawOverrunMillis=$rawOverrunMillis overrunMillis=$overrunMillis " +
                "necessaryUseExcessMillis=$necessaryUseExcessMillis " +
                "outcomeType=${session.outcomeType?.name} extensionCount=${session.extensionCount} " +
                "modeAtStart=${session.modeAtStart} " +
                "isVulnerableTimeAtStart=${session.isVulnerableTimeAtStart} " +
                "interventionAppliedAtStart=${session.interventionAppliedAtStart}"
        )

        val entity = SessionLogEntity(
            packageName = session.packageName,
            appDisplayName = resolveAppDisplayName(session.packageName),
            entryDetectedAtMillis = session.entryDetectedAtMillis,
            startedAtMillis = session.startedAtMillis,
            endedAtMillis = endedAtMillis,
            durationMillis = durationMillis,
            targetDurationMillis = session.targetDurationMillis,
            interventionVisibleMillis = interventionVisibleMillis,
            effectiveUsageMillis = effectiveUsageMillis,
            totalExtensionDurationMillis = totalExtensionDurationMillis,
            finalTargetDurationMillis = finalTargetDurationMillis,
            rawOverrunMillis = rawOverrunMillis,
            overrunDetectedAtMillis = overrunDetectedAtMillis,
            overrunMillis = overrunMillis,
            necessaryUseExcessMillis = necessaryUseExcessMillis,
            intentChoice = session.intentChoice.name,
            modeAtStart = session.modeAtStart,
            isVulnerableTimeAtStart = session.isVulnerableTimeAtStart,
            interventionAppliedAtStart = session.interventionAppliedAtStart,
            outcomeType = session.outcomeType?.name,
            outcomeRespondedAtMillis = session.outcomeType?.let { savedAtMillis },
            outcomeAchieved = when (session.outcomeType) {
                OutcomeType.PURPOSE_ACHIEVED -> true
                OutcomeType.NECESSARY_USE,
                OutcomeType.PURPOSE_DRIFT,
                OutcomeType.CONTINUED_SCROLLING -> false
                else -> null
            },
            purposeDrifted = when (session.outcomeType) {
                OutcomeType.PURPOSE_ACHIEVED,
                OutcomeType.NECESSARY_USE -> false
                OutcomeType.PURPOSE_DRIFT,
                OutcomeType.CONTINUED_SCROLLING -> true
                else -> null
            },
            overrun = overrunMillis > 0L,
            extensionCount = session.extensionCount,
            cautionExtensionCount = session.cautionExtensionCount,
            interventionEverApplied = session.interventionEverApplied,
            isFastReopen = reopenCheck.isFastReopen,
            reopenGapMillis = reopenCheck.gapTimeMillis,
            createdAtMillis = savedAtMillis
        )
        return database.withTransaction {
            val currentSessionId = sessionLogDao.insertSessionLog(entity)
            if (session.extensionEvents.isNotEmpty()) {
                extensionEventDao.insertExtensionEvents(
                    session.extensionEvents.map { event ->
                        ExtensionEventEntity(
                            sessionId = currentSessionId,
                            occurredAtMillis = event.occurredAtMillis,
                            extensionDurationMillis = event.extensionDurationMillis,
                            interventionActiveAtTime = event.interventionActiveAtTime
                        )
                    }
                )
            }
            val previousSessionId = reopenCheck.previousSessionId
            val gapTimeMillis = reopenCheck.gapTimeMillis
            if (previousSessionId != null && gapTimeMillis != null) {
                reopenLogDao.insertReopenLog(
                    ReopenLogEntity(
                        previousSessionId = previousSessionId,
                        currentSessionId = currentSessionId,
                        gapTimeMillis = gapTimeMillis,
                        isFastReopen = reopenCheck.isFastReopen
                    )
                )
            }
            currentSessionId
        }
    }

    private fun resolveAppDisplayName(packageName: String): String? {
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString().takeIf { it.isNotBlank() }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    suspend fun checkReopen(
        packageName: String,
        currentEntryTimeMillis: Long,
        thresholdMillis: Long = DEFAULT_REOPEN_THRESHOLD_MILLIS
    ): ReopenCheckResult {
        val previousSession = sessionLogDao.findLatestEndedSession(
            packageName = packageName,
            beforeMillis = currentEntryTimeMillis
        ) ?: return ReopenCheckResult(
            previousSessionId = null,
            previousEndTimeMillis = null,
            gapTimeMillis = null,
            isFastReopen = false
        )
        val gapTimeMillis =
            (currentEntryTimeMillis - previousSession.endedAtMillis).coerceAtLeast(0L)
        return ReopenCheckResult(
            previousSessionId = previousSession.id,
            previousEndTimeMillis = previousSession.endedAtMillis,
            gapTimeMillis = gapTimeMillis,
            isFastReopen = gapTimeMillis <= thresholdMillis
        )
    }

    fun observeRecentSessions(limit: Int = 10): Flow<List<SessionLogEntity>> {
        return sessionLogDao.observeRecentSessions(limit)
    }

    fun observeTodaySessionCount(startOfDayMillis: Long = startOfTodayMillis()): Flow<Int> {
        return sessionLogDao.observeTodaySessionCount(startOfDayMillis)
    }

    fun observeTodayOverrunCount(startOfDayMillis: Long = startOfTodayMillis()): Flow<Int> {
        return sessionLogDao.observeTodayOverrunCount(startOfDayMillis)
    }

    fun observeTodayFastReopenCount(startOfDayMillis: Long = startOfTodayMillis()): Flow<Int> {
        return sessionLogDao.observeTodayFastReopenCount(startOfDayMillis)
    }

    fun observeTodayPurposeDriftCount(startOfDayMillis: Long = startOfTodayMillis()): Flow<Int> {
        return sessionLogDao.observeTodayPurposeDriftCount(startOfDayMillis)
    }

    fun observeSessionsForLastSevenDays(): Flow<List<SessionLogEntity>> {
        return sessionLogDao.observeSessionsSince(lastSevenDaysSinceMillis())
    }

    fun observeOpenEventsForLastSevenDays(): Flow<List<AppOpenEventEntity>> {
        return appOpenEventDao.observeOpenEventsSince(lastSevenDaysSinceMillis())
    }

    fun observeSessionsSince(sinceMillis: Long): Flow<List<SessionLogEntity>> {
        return sessionLogDao.observeSessionsSince(sinceMillis)
    }

    fun observeSessionsOverlappingPeriod(
        sinceMillis: Long,
        untilMillis: Long
    ): Flow<List<SessionLogEntity>> {
        return sessionLogDao.observeSessionsOverlappingPeriod(
            sinceMillis = sinceMillis,
            untilMillis = untilMillis
        )
    }

    fun observeOpenEventsSince(sinceMillis: Long): Flow<List<AppOpenEventEntity>> {
        return appOpenEventDao.observeOpenEventsSince(sinceMillis)
    }

    fun observeReopenLogsSince(
        sinceMillis: Long,
        packageName: String? = null
    ): Flow<List<ReopenLogEntity>> {
        return reopenLogDao.observeReopenLogsSince(sinceMillis, packageName)
    }

    fun observeTodayCloseNowBeforeSessionCount(
        startOfDayMillis: Long = startOfTodayMillis()
    ): Flow<Int> {
        return selfControlEventDao.observeEventCountByTypeSince(
            eventType = EVENT_CLOSE_NOW_BEFORE_SESSION,
            sinceMillis = startOfDayMillis
        )
    }

    suspend fun deleteAllSessionLogs() {
        database.withTransaction {
            sessionLogDao.deleteAllSessionLogs()
            appOpenEventDao.deleteAllAppOpenEvents()
            selfControlEventDao.deleteAllSelfControlEvents()
        }
    }

    suspend fun updateSessionOutcome(
        sessionId: Long,
        outcomeType: OutcomeType,
        respondedAtMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val achieved = outcomeType == OutcomeType.PURPOSE_ACHIEVED
        val drifted = outcomeType == OutcomeType.PURPOSE_DRIFT ||
            outcomeType == OutcomeType.CONTINUED_SCROLLING
        return sessionLogDao.updateOutcome(
            sessionId = sessionId,
            outcomeType = outcomeType.name,
            respondedAtMillis = respondedAtMillis,
            achieved = achieved,
            drifted = drifted
        ) == 1
    }

    suspend fun confirmExitAfterIntervention(
        sessionId: Long,
        confirmedAtMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return sessionLogDao.confirmExitAfterIntervention(
            sessionId = sessionId,
            confirmedAtMillis = confirmedAtMillis,
            clearPurpose = IntentChoice.CLEAR_PURPOSE.name,
            purposeAchieved = OutcomeType.PURPOSE_ACHIEVED.name
        ) == 1
    }

    companion object {
        const val DEFAULT_REOPEN_THRESHOLD_MILLIS = 5L * 60L * 1_000L
        const val EVENT_CLOSE_NOW_BEFORE_SESSION = "CLOSE_NOW_BEFORE_SESSION"

        fun startOfTodayMillis(): Long {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        fun lastSevenDaysSinceMillis(): Long {
            return System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1_000L
        }
    }
}
