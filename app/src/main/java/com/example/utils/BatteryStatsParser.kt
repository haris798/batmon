package com.example.utils

import android.content.Intent
import android.os.BatteryManager
import kotlin.math.abs

object BatteryStatsParser {

    /** Fallback voltage (3.7V) typical for Li-ion if sensor missing */
    const val FALLBACK_VOLTAGE_MV = 3700
    /** Fallback temperature (25.0 C) */
    const val FALLBACK_TEMP_C = 25.0f

    /**
     * Safely parses the battery percentage from the intent.
     * Returns 0f if unable to parse.
     */
    fun parseBatteryPercentage(intent: Intent?): Float {
        if (intent == null) return 0f
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level != -1 && scale > 0) {
            (level.toFloat() / scale.toFloat()) * 100f
        } else {
            0f
        }
    }

    /**
     * Safely parses the battery voltage in mV.
     * Returns FALLBACK_VOLTAGE_MV if the sensor data is missing or invalid.
     */
    fun parseVoltageMv(intent: Intent?): Int {
        if (intent == null) return FALLBACK_VOLTAGE_MV
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        return if (voltage > 0) voltage else FALLBACK_VOLTAGE_MV
    }

    /**
     * Safely parses the battery temperature in Celsius.
     * Returns FALLBACK_TEMP_C if the sensor data is missing.
     */
    fun parseTemperatureC(intent: Intent?): Float {
        if (intent == null) return FALLBACK_TEMP_C
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        return if (tempTenths != -1) {
            tempTenths / 10f
        } else {
            FALLBACK_TEMP_C
        }
    }

    /**
     * Safely determines if the battery is currently charging or full.
     */
    fun parseIsCharging(intent: Intent?): Boolean {
        if (intent == null) return false
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Calculates the power in Watts from voltage in mV and current in mA.
     * Includes a safeguard against anomalous or 0 voltage readings.
     */
    fun calculatePowerW(voltageMv: Int, currentMa: Int): Float {
        val safeVoltageMv = if (voltageMv > 0) voltageMv else FALLBACK_VOLTAGE_MV
        return (safeVoltageMv / 1000f) * (currentMa / 1000f)
    }
}
