package com.teum.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reopen_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["previousSessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["currentSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("previousSessionId"),
        Index("currentSessionId")
    ]
)
data class ReopenLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val previousSessionId: Long,
    val currentSessionId: Long,
    val gapTimeMillis: Long,
    val isFastReopen: Boolean
)
