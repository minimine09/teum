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

    @Query("SELECT COUNT(*) FROM session_logs WHERE startedAtMillis >= :startOfDayMillis AND outcomeType = 'PURPOSE_DRIFT'")
    fun observeTodayPurposeDriftCount(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT * FROM session_logs WHERE startedAtMillis >= :sinceMillis ORDER BY startedAtMillis DESC")
    fun observeSessionsSince(sinceMillis: Long): Flow<List<SessionLogEntity>>

    @Query("DELETE FROM session_logs")
    suspend fun deleteAllSessionLogs()
}
