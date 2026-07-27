package com.teum.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "self_control_events")
data class SelfControlEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String,
    val appDisplayName: String?,
    val eventType: String,
    val occurredAtMillis: Long,
    val modeAtTime: String?,
    val isVulnerableTimeAtTime: Boolean,
    val interventionActiveAtTime: Boolean,
    val source: String?
)
