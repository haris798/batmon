package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_samples")
data class BatterySampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val batteryPercent: Float,
    val voltageMv: Int,
    val currentMa: Int,
    val powerMw: Int,
    val temperatureC: Float,
    val screenOn: Boolean,
    val charging: Boolean,
    val foregroundPackage: String? = null
)
