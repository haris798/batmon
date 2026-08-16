package com.example.domain

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.data.local.BatterySampleEntity
import com.example.data.provider.SystemDataProvider

data class UsageStats(
    val durationMs: Long,
    val totalPercentConsumed: Float,
    val totalCapacityMahConsumed: Float,
    
    val screenOnDurationMs: Long,
    val screenOnPercent: Float,
    val screenOnCapacityMah: Float,
    val screenOnDrainRatePerHr: Float, // % per hour
    
    val screenOffDurationMs: Long,
    val screenOffPercent: Float,
    val screenOffCapacityMah: Float,
    val screenOffDrainRatePerHr: Float, // % per hour
    
    val combinedDrainRatePerHr: Float, // % per hour
    
    val deepSleepDurationMs: Long?, // Null if unsupported
    
    val appUsage: List<AppUsageEstimate>
)

data class AppUsageEstimate(
    val appName: String,
    val packageName: String,
    val percentage: Float, // Estimated percentage of total battery drain
    val capacityMah: Float, // Estimated mAh consumed
    val usageTimeMs: Long // Real time spent in foreground
)

class BatteryUsageEstimator(
    private val context: Context,
    private val dataProvider: SystemDataProvider
) {
    fun estimateUsage(samples: List<BatterySampleEntity>): UsageStats {
        if (samples.isEmpty() || samples.size == 1) {
            return emptyUsageStats()
        }

        val sortedSamples = samples.sortedBy { it.timestamp }
        val startSample = sortedSamples.first()
        val endSample = sortedSamples.last()
        
        val durationMs = endSample.timestamp - startSample.timestamp
        val percentConsumed = startSample.batteryPercent - endSample.batteryPercent
        
        val nominalCapacity = dataProvider.getNominalCapacity() ?: 5000 // Default 5000 mAh if unknown
        val mahConsumed = (percentConsumed / 100f) * nominalCapacity
        
        var screenOnMs = 0L
        var screenOffMs = 0L
        var screenOnPercent = 0f
        var screenOffPercent = 0f
        
        val appDrainMap = mutableMapOf<String, Float>()
        
        for (i in 0 until sortedSamples.size - 1) {
            val current = sortedSamples[i]
            val next = sortedSamples[i+1]
            val intervalMs = next.timestamp - current.timestamp
            val intervalDrop = current.batteryPercent - next.batteryPercent
            
            if (current.screenOn) {
                screenOnMs += intervalMs
                if (intervalDrop > 0) screenOnPercent += intervalDrop
                
                // Attribute drain to foreground app
                val app = current.foregroundPackage ?: "Unknown"
                appDrainMap[app] = appDrainMap.getOrDefault(app, 0f) + (intervalDrop.coerceAtLeast(0f))
            } else {
                screenOffMs += intervalMs
                if (intervalDrop > 0) screenOffPercent += intervalDrop
            }
        }
        
        val screenOnDrainRate = if (screenOnMs > 0) (screenOnPercent / (screenOnMs / 3600000f)) else 0f
        val screenOffDrainRate = if (screenOffMs > 0) (screenOffPercent / (screenOffMs / 3600000f)) else 0f
        val combinedDrainRate = if (durationMs > 0) (percentConsumed / (durationMs / 3600000f)) else 0f
        
        // Fetch accurate usage time from UsageStatsManager
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(startSample.timestamp, endSample.timestamp)
        
        val appUsageList = appDrainMap.map { (pkg, drop) ->
            val timeMs = usageStatsMap[pkg]?.totalTimeInForeground ?: 0L
            AppUsageEstimate(
                appName = getAppName(pkg),
                packageName = pkg,
                percentage = drop,
                capacityMah = (drop / 100f) * nominalCapacity,
                usageTimeMs = timeMs
            )
        }.sortedByDescending { it.percentage }

        return UsageStats(
            durationMs = durationMs,
            totalPercentConsumed = percentConsumed.coerceAtLeast(0f),
            totalCapacityMahConsumed = mahConsumed.coerceAtLeast(0f),
            
            screenOnDurationMs = screenOnMs,
            screenOnPercent = screenOnPercent,
            screenOnCapacityMah = (screenOnPercent / 100f) * nominalCapacity,
            screenOnDrainRatePerHr = screenOnDrainRate,
            
            screenOffDurationMs = screenOffMs,
            screenOffPercent = screenOffPercent,
            screenOffCapacityMah = (screenOffPercent / 100f) * nominalCapacity,
            screenOffDrainRatePerHr = screenOffDrainRate,
            
            combinedDrainRatePerHr = combinedDrainRate,
            deepSleepDurationMs = null,
            appUsage = appUsageList
        )
    }
    
    private fun emptyUsageStats() = UsageStats(
        durationMs = 0, totalPercentConsumed = 0f, totalCapacityMahConsumed = 0f,
        screenOnDurationMs = 0, screenOnPercent = 0f, screenOnCapacityMah = 0f, screenOnDrainRatePerHr = 0f,
        screenOffDurationMs = 0, screenOffPercent = 0f, screenOffCapacityMah = 0f, screenOffDrainRatePerHr = 0f,
        combinedDrainRatePerHr = 0f, deepSleepDurationMs = null, appUsage = emptyList()
    )
    
    private fun getAppName(packageName: String): String {
        if (packageName == "Unknown") return "Unknown System/App"
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
