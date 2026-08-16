package com.example

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.BatteryDatabase
import com.example.data.provider.SystemDataProviderImpl
import com.example.data.repository.BatteryRepository
import com.example.domain.BatteryUsageEstimator
import com.example.worker.BatteryWorkerFactory
import java.util.concurrent.TimeUnit
import androidx.room.Room

class BatteryMonitorApplication : Application(), Configuration.Provider {
    
    lateinit var repository: BatteryRepository
    lateinit var estimator: BatteryUsageEstimator

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(BatteryWorkerFactory(repository))
            .build()
            
    override fun onCreate() {
        super.onCreate()
        
        val db = Room.databaseBuilder(this, BatteryDatabase::class.java, "battery_monitor.db").fallbackToDestructiveMigration().build()
        val dataProvider = SystemDataProviderImpl(this)
        repository = BatteryRepository(db.batteryDao(), dataProvider)
        estimator = BatteryUsageEstimator(this, dataProvider)
        
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(false) // We want to track battery even when low
            .build()
            
        val workRequest = PeriodicWorkRequestBuilder<com.example.worker.BatteryMonitorWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BatteryMonitorWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
