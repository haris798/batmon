package com.example.data.provider

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

interface SystemDataProvider {
    fun getBatteryPercentage(): Float
    fun getVoltageMv(): Int
    fun getCurrentMa(): Int
    fun getTemperatureC(): Float
    fun isCharging(): Boolean
    fun isScreenOn(): Boolean
    fun getForegroundPackage(timeRangeMs: Long = 10000L): String?
    fun getNominalCapacity(): Int?
}

class SystemDataProviderImpl(private val context: Context) : SystemDataProvider {

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    override fun getBatteryPercentage(): Float {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) {
            (level.toFloat() / scale.toFloat()) * 100f
        } else {
            0f
        }
    }

    override fun getVoltageMv(): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
    }

    override fun getCurrentMa(): Int {
        val currentUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        var currentMa = currentUa / 1000 
        
        if (!isCharging() && currentMa > 0) {
            currentMa = -currentMa
        }
        
        if (currentMa == 0 && currentUa != 0 && Math.abs(currentUa) < 10000) {
            currentMa = currentUa 
        }
        
        return currentMa
    }

    override fun getTemperatureC(): Float {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return if (tempTenths != -1) {
            tempTenths / 10f
        } else {
            0f
        }
    }

    override fun isCharging(): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    override fun isScreenOn(): Boolean {
        return powerManager.isInteractive
    }

    override fun getForegroundPackage(timeRangeMs: Long): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - timeRangeMs
        
        var foregroundPackage: String? = null
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundPackage = event.packageName
            } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                if (event.packageName == foregroundPackage) {
                    foregroundPackage = null
                }
            }
        }
        
        return foregroundPackage
    }
    
    override fun getNominalCapacity(): Int? {
        val chargeCounterUah = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val percentage = getBatteryPercentage()
        
        if (chargeCounterUah > 0 && percentage > 0) {
            val currentMah = chargeCounterUah / 1000
            return ((currentMah / (percentage / 100f))).toInt()
        }
        return null
    }
}
