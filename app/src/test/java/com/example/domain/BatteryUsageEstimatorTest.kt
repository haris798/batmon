package com.example.domain

import android.content.Context
import android.content.pm.PackageManager
import com.example.data.local.BatterySampleEntity
import com.example.data.provider.SystemDataProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class BatteryUsageEstimatorTest {

    private lateinit var estimator: BatteryUsageEstimator
    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    private lateinit var mockDataProvider: SystemDataProvider

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPackageManager = mock(PackageManager::class.java)
        mockDataProvider = mock(SystemDataProvider::class.java)

        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockDataProvider.getNominalCapacity()).thenReturn(5000)

        estimator = BatteryUsageEstimator(mockContext, mockDataProvider)
    }

    @Test
    fun `estimateUsage with empty samples returns empty stats`() {
        val stats = estimator.estimateUsage(emptyList())
        assertEquals(0f, stats.totalPercentConsumed)
    }

    @Test
    fun `estimateUsage with samples calculates correct consumption`() {
        val samples = listOf(
            BatterySampleEntity(
                timestamp = 1000L,
                batteryPercent = 100f,
                voltageMv = 4000,
                currentMa = -500,
                powerMw = 2000,
                temperatureC = 35f,
                screenOn = true,
                charging = false,
                foregroundPackage = "com.example.app1"
            ),
            BatterySampleEntity(
                timestamp = 2000L,
                batteryPercent = 90f,
                voltageMv = 3900,
                currentMa = -400,
                powerMw = 1560,
                temperatureC = 36f,
                screenOn = true,
                charging = false,
                foregroundPackage = "com.example.app1"
            ),
            BatterySampleEntity(
                timestamp = 3000L,
                batteryPercent = 85f,
                voltageMv = 3800,
                currentMa = -100,
                powerMw = 380,
                temperatureC = 34f,
                screenOn = false,
                charging = false,
                foregroundPackage = null
            )
        )

        val stats = estimator.estimateUsage(samples)
        
        // 100 -> 90 = 10%, 90 -> 85 = 5%, Total = 15%
        assertEquals(15f, stats.totalPercentConsumed)
        assertEquals(10f, stats.screenOnPercent)
        assertEquals(5f, stats.screenOffPercent)
        
        // Duration: 1000L to 2000L is screenOn (1000ms), 2000L to 3000L is screenOff (1000ms)
        assertEquals(1000L, stats.screenOnDurationMs)
        assertEquals(1000L, stats.screenOffDurationMs)
    }
}
