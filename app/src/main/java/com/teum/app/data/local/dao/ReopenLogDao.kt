package com.teum.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teum.app.data.local.entity.ReopenLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReopenLogDao {
    @Insert
    suspend fun insertReopenLog(entity: ReopenLogEntity): Long

    @Query(
        """
        SELECT reopen_logs.*
        FROM reopen_logs
        INNER JOIN session_logs
            ON session_logs.id = reopen_logs.currentSessionId
        WHERE session_logs.startedAtMillis >= :sinceMillis
            AND (:packageName IS NULL OR session_logs.packageName = :packageName)
        ORDER BY session_logs.startedAtMillis DESC
        """
    )
    fun observeReopenLogsSince(
        sinceMillis: Long,
        packageName: String?
    ): Flow<List<ReopenLogEntity>>
}
