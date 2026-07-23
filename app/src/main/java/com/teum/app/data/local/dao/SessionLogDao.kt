package com.teum.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teum.app.data.local.entity.SessionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLogDao {
    @Insert
    suspend fun insertSessionLog(entity: SessionLogEntity): Long

    @Query("SELECT * FROM session_logs ORDER BY endedAtMillis DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int = 10): Flow<List<SessionLogEntity>>

    @Query("SELECT COUNT(*) FROM session_logs WHERE startedAtMillis >= :startOfDayMillis")
    fun observeTodaySessionCount(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM session_logs WHERE startedAtMillis >= :startOfDayMillis AND overrun = 1")
    fun observeTodayOverrunCount(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM session_logs WHERE startedAtMillis >= :startOfDayMillis AND isFastReopen = 1")
    fun observeTodayFastReopenCount(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM session_logs WHERE startedAtMillis >= :startOfDayMillis AND purposeDrifted = 1")
    fun observeTodayPurposeDriftCount(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT * FROM session_logs WHERE startedAtMillis >= :sinceMillis ORDER BY startedAtMillis DESC")
    fun observeSessionsSince(sinceMillis: Long): Flow<List<SessionLogEntity>>

    @Query(
        """
        SELECT *
        FROM session_logs
        WHERE packageName = :packageName
            AND endedAtMillis <= :beforeMillis
        ORDER BY endedAtMillis DESC
        LIMIT 1
        """
    )
    suspend fun findLatestEndedSession(
        packageName: String,
        beforeMillis: Long
    ): SessionLogEntity?

    @Query("DELETE FROM session_logs")
    suspend fun deleteAllSessionLogs()

    @Query(
        """
        UPDATE session_logs
        SET outcomeType = :outcomeType,
            outcomeRespondedAtMillis = :respondedAtMillis,
            outcomeAchieved = :achieved,
            purposeDrifted = :drifted,
            necessaryUseExcessMillis = CASE
                WHEN intentChoice = 'CLEAR_PURPOSE' AND :outcomeType = 'NECESSARY_USE'
                    THEN rawOverrunMillis
                ELSE 0
            END,
            overrunMillis = rawOverrunMillis,
            overrun = CASE
                WHEN rawOverrunMillis > 0 THEN 1
                ELSE 0
            END
        WHERE id = :sessionId
        """
    )
    suspend fun updateOutcome(
        sessionId: Long,
        outcomeType: String,
        respondedAtMillis: Long,
        achieved: Boolean,
        drifted: Boolean
    ): Int

    @Query(
        """
        UPDATE session_logs
        SET closedAfterIntervention = 1,
            interventionExitConfirmedAtMillis = :confirmedAtMillis
        WHERE id = :sessionId
        """
    )
    suspend fun confirmExitAfterIntervention(
        sessionId: Long,
        confirmedAtMillis: Long
    ): Int
}
