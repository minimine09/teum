package com.teum.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teum.app.data.local.entity.ExtensionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtensionEventDao {
    @Insert
    suspend fun insertExtensionEvent(entity: ExtensionEventEntity): Long

    @Insert
    suspend fun insertExtensionEvents(entities: List<ExtensionEventEntity>): List<Long>

    @Query("SELECT * FROM extension_events ORDER BY occurredAtMillis ASC")
    suspend fun getAllExtensionEventsForDebug(): List<ExtensionEventEntity>

    @Query("SELECT COUNT(*) FROM extension_events")
    suspend fun countExtensionEventsForDebug(): Int

    @Query("DELETE FROM extension_events")
    suspend fun deleteAllExtensionEventsForDebug()

    @Query(
        "SELECT * FROM extension_events " +
            "WHERE occurredAtMillis >= :sinceMillis " +
            "ORDER BY occurredAtMillis DESC"
    )
    fun observeExtensionEventsSince(sinceMillis: Long): Flow<List<ExtensionEventEntity>>

    @Query(
        "SELECT * FROM extension_events " +
            "WHERE sessionId = :sessionId " +
            "ORDER BY occurredAtMillis ASC"
    )
    fun observeExtensionEventsForSession(sessionId: Long): Flow<List<ExtensionEventEntity>>
}
