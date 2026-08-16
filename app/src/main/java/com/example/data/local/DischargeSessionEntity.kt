package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discharge_sessions")
data class DischargeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val startLevel: Float,
    val endLevel: Float? = null,
    val durationMs: Long = 0,
    val batteryConsumed: Float = 0f
)
