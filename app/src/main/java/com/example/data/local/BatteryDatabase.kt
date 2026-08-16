package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BatterySampleEntity::class, DischargeSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BatteryDatabase : RoomDatabase() {
    abstract fun batteryDao(): BatteryDao
}
