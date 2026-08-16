package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: BatterySampleEntity)
    
    @Query("SELECT * FROM battery_samples ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSample(): Flow<BatterySampleEntity?>
    
    @Query("SELECT * FROM battery_samples WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getSamplesSince(since: Long): List<BatterySampleEntity>
    
    @Query("SELECT * FROM battery_samples WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getSamplesSinceFlow(since: Long): Flow<List<BatterySampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DischargeSessionEntity): Long
    
    @Update
    suspend fun updateSession(session: DischargeSessionEntity)
    
    @Query("SELECT * FROM discharge_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSession(): DischargeSessionEntity?
    
    @Query("SELECT * FROM discharge_sessions ORDER BY startTime DESC LIMIT 1")
    fun getLatestSessionFlow(): Flow<DischargeSessionEntity?>
    
    @Query("SELECT * FROM discharge_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<DischargeSessionEntity>>
    
    @Query("DELETE FROM battery_samples WHERE timestamp < :threshold")
    suspend fun deleteOldSamples(threshold: Long)
}
