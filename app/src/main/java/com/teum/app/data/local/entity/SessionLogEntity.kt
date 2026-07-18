package com.teum.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String,
    val entryDetectedAtMillis: Long,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationMillis: Long,
    val targetDurationMillis: Long,
    val intentChoice: String,
    val outcomeType: String?,
    val outcomeRespondedAtMillis: Long? = null,
    val outcomeAchieved: Boolean? = null,
    val purposeDrifted: Boolean? = null,
    val closedAfterIntervention: Boolean? = null,
    val interventionExitConfirmedAtMillis: Long? = null,
    val overrun: Boolean,
    val extensionCount: Int,
    val isFastReopen: Boolean,
    val reopenGapMillis: Long?,
    val createdAtMillis: Long
)
