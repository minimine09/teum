package com.teum.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teum.app.data.local.entity.SelfControlEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SelfControlEventDao {
    @Insert
    suspend fun insertSelfControlEvent(entity: SelfControlEventEntity): Long

    @Query("SELECT * FROM self_control_events ORDER BY occurredAtMillis ASC")
    suspend fun getAllSelfControlEventsForDebug(): List<SelfControlEventEntity>

    @Query("SELECT COUNT(*) FROM self_control_events")
    suspend fun countSelfControlEventsForDebug(): Int

    @Query(
        "SELECT COUNT(*) FROM self_control_events " +
            "WHERE eventType = :eventType AND occurredAtMillis >= :sinceMillis"
    )
    fun observeEventCountByTypeSince(eventType: String, sinceMillis: Long): Flow<Int>

    @Query("DELETE FROM self_control_events")
    suspend fun deleteAllSelfControlEvents()
}
