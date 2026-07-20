package com.teum.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teum.app.data.local.entity.AppOpenEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppOpenEventDao {
    @Insert
    suspend fun insertAppOpenEvent(entity: AppOpenEventEntity): Long

    @Query("SELECT * FROM app_open_events WHERE detectedAtMillis >= :sinceMillis ORDER BY detectedAtMillis DESC")
    fun observeOpenEventsSince(sinceMillis: Long): Flow<List<AppOpenEventEntity>>

    @Query("DELETE FROM app_open_events")
    suspend fun deleteAllAppOpenEvents()
}
