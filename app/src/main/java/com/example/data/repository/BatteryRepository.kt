package com.example.data.repository

import com.example.data.local.BatteryDao
import com.example.data.local.BatterySampleEntity
import com.example.data.local.DischargeSessionEntity
import com.example.data.provider.SystemDataProvider
import kotlinx.coroutines.flow.Flow

class BatteryRepository(
    private val batteryDao: BatteryDao,
    private val dataProvider: SystemDataProvider
) {

    fun getLatestSample(): Flow<BatterySampleEntity?> = batteryDao.getLatestSample()

    fun getLatestSessionFlow(): Flow<DischargeSessionEntity?> = batteryDao.getLatestSessionFlow()

    suspend fun getSamplesSince(timestamp: Long): List<BatterySampleEntity> = batteryDao.getSamplesSince(timestamp)

    suspend fun purgeOldSamples() {
        val thirtyDaysInMillis = 30L * 24L * 60L * 60L * 1000L
        val threshold = System.currentTimeMillis() - thirtyDaysInMillis
        batteryDao.deleteOldSamples(threshold)
    }

    suspend fun recordSample() {
        val currentMa = dataProvider.getCurrentMa()
        val voltageMv = dataProvider.getVoltageMv()
        val powerMw = (currentMa.toFloat() * voltageMv.toFloat() / 1000f).toInt()
        
        val sample = BatterySampleEntity(
            timestamp = System.currentTimeMillis(),
            batteryPercent = dataProvider.getBatteryPercentage(),
            voltageMv = voltageMv,
            currentMa = currentMa,
            powerMw = powerMw,
            temperatureC = dataProvider.getTemperatureC(),
            charging = dataProvider.isCharging(),
            screenOn = dataProvider.isScreenOn(),
            foregroundPackage = if (dataProvider.isScreenOn()) dataProvider.getForegroundPackage() else null
        )
        
        batteryDao.insertSample(sample)
        
        // Session management logic
        val latestSession = batteryDao.getLatestSession()
        
        if (sample.charging) {
            // End active session if charging
            if (latestSession != null && latestSession.endTime == null) {
                batteryDao.updateSession(latestSession.copy(endTime = sample.timestamp))
            }
        } else {
            // Start new session if discharging and no active session
            if (latestSession == null || latestSession.endTime != null) {
                batteryDao.insertSession(
                    DischargeSessionEntity(
                        startTime = sample.timestamp,
                        startLevel = sample.batteryPercent
                    )
                )
            }
        }
    }
}
