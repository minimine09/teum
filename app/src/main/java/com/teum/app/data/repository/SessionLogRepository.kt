package com.teum.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.teum.app.data.local.TeumDatabase
import com.teum.app.data.local.entity.AppOpenEventEntity
import com.teum.app.data.local.entity.SessionLogEntity
import com.teum.app.session.AppSession
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SessionLogRepository(context: Context) {
    private val database = TeumDatabase.getInstance(context)
    private val sessionLogDao = database.sessionLogDao()
    private val appOpenEventDao = database.appOpenEventDao()

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

    suspend fun saveEndedSession(session: AppSession): Long? {
        val endedAtMillis = session.endedAtMillis ?: return null
        val durationMillis = (endedAtMillis - session.startedAtMillis).coerceAtLeast(0L)
        val entity = SessionLogEntity(
            packageName = session.packageName,
            entryDetectedAtMillis = session.entryDetectedAtMillis,
            startedAtMillis = session.startedAtMillis,
            endedAtMillis = endedAtMillis,
            durationMillis = durationMillis,
            targetDurationMillis = session.targetDurationMillis,
            intentChoice = session.intentChoice.name,
            outcomeType = session.outcomeType?.name,
            overrun = durationMillis > session.targetDurationMillis,
            extensionCount = session.extensionCount,
            isFastReopen = session.isFastReopen,
            reopenGapMillis = session.reopenGapMillis,
            createdAtMillis = System.currentTimeMillis()
        )
        return sessionLogDao.insertSessionLog(entity)
    }

    fun observeRecentSessions(limit: Int = 10): Flow<List<SessionLogEntity>> {
        return sessionLogDao.observeRecentSessions(limit)
    }

    fun observeTodaySessionCount(): Flow<Int> {
        return sessionLogDao.observeTodaySessionCount(startOfTodayMillis())
    }

    fun observeTodayOverrunCount(): Flow<Int> {
        return sessionLogDao.observeTodayOverrunCount(startOfTodayMillis())
    }

    fun observeTodayFastReopenCount(): Flow<Int> {
        return sessionLogDao.observeTodayFastReopenCount(startOfTodayMillis())
    }

    fun observeTodayPurposeDriftCount(): Flow<Int> {
        return sessionLogDao.observeTodayPurposeDriftCount(startOfTodayMillis())
    }

    fun observeSessionsForLastSevenDays(): Flow<List<SessionLogEntity>> {
        return sessionLogDao.observeSessionsSince(lastSevenDaysSinceMillis())
    }

    fun observeOpenEventsForLastSevenDays(): Flow<List<AppOpenEventEntity>> {
        return appOpenEventDao.observeOpenEventsSince(lastSevenDaysSinceMillis())
    }

    suspend fun deleteAllSessionLogs() {
        database.withTransaction {
            sessionLogDao.deleteAllSessionLogs()
            appOpenEventDao.deleteAllAppOpenEvents()
        }
    }

    companion object {
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
