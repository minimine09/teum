package com.teum.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.teum.app.data.local.TeumDatabase
import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.ReopenLogEntity
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.overlay.BrakeChoice
import com.teum.app.overlay.IntentChoice
import com.teum.app.session.AppSession
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SessionLogRepository(context: Context) {
    private val database = TeumDatabase.getInstance(context)
    private val sessionLogDao = database.sessionLogDao()
    private val appOpenEventDao = database.appOpenEventDao()
    private val reopenLogDao = database.reopenLogDao()

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

    suspend fun saveEndedSession(
        session: AppSession,
        brakeChoice: BrakeChoice? = null
    ): Long? {
        val endedAtMillis = session.endedAtMillis ?: return null
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
        val isNecessaryUseException =
            session.intentChoice == IntentChoice.CLEAR_PURPOSE &&
                brakeChoice == BrakeChoice.NECESSARY_USE
        val overrunMillis = if (isNecessaryUseException) 0L else rawOverrunMillis
        val necessaryUseExcessMillis =
            if (isNecessaryUseException) rawOverrunMillis else 0L

        com.teum.app.debug.TeumLogger.session(
            debugSessionId = session.debugSessionId,
            event = "DB_SAVE_VALUES",
            detail = "duration=$durationMillis intervention=$interventionVisibleMillis " +
                "effective=$effectiveUsageMillis initialTarget=${session.targetDurationMillis} " +
                "extensionTotal=$totalExtensionDurationMillis finalTarget=$finalTargetDurationMillis " +
                "rawOverrunMillis=$rawOverrunMillis overrunMillis=$overrunMillis " +
                "necessaryUseExcessMillis=$necessaryUseExcessMillis " +
                "brakeChoice=${brakeChoice?.name} extensionCount=${session.extensionCount}"
        )

        val entity = SessionLogEntity(
            packageName = session.packageName,
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
            overrunMillis = overrunMillis,
            necessaryUseExcessMillis = necessaryUseExcessMillis,
            intentChoice = session.intentChoice.name,
            brakeChoice = brakeChoice?.name,
            outcomeType = session.outcomeType?.name,
            overrun = overrunMillis > 0L,
            extensionCount = session.extensionCount,
            isFastReopen = reopenCheck.isFastReopen,
            reopenGapMillis = reopenCheck.gapTimeMillis,
            createdAtMillis = System.currentTimeMillis()
        )
        return database.withTransaction {
            val currentSessionId = sessionLogDao.insertSessionLog(entity)
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

    fun observeOpenEventsSince(sinceMillis: Long): Flow<List<AppOpenEventEntity>> {
        return appOpenEventDao.observeOpenEventsSince(sinceMillis)
    }

    fun observeReopenLogsSince(
        sinceMillis: Long,
        packageName: String? = null
    ): Flow<List<ReopenLogEntity>> {
        return reopenLogDao.observeReopenLogsSince(sinceMillis, packageName)
    }

    suspend fun deleteAllSessionLogs() {
        database.withTransaction {
            sessionLogDao.deleteAllSessionLogs()
            appOpenEventDao.deleteAllAppOpenEvents()
        }
    }

    suspend fun updateSessionOutcome(
        sessionId: Long,
        outcomeType: String,
        achieved: Boolean,
        drifted: Boolean,
        respondedAtMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return sessionLogDao.updateOutcome(
            sessionId = sessionId,
            outcomeType = outcomeType,
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
            confirmedAtMillis = confirmedAtMillis
        ) == 1
    }

    companion object {
        const val DEFAULT_REOPEN_THRESHOLD_MILLIS = 5L * 60L * 1_000L

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
