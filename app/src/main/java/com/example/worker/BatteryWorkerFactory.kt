package com.example.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.data.repository.BatteryRepository

class BatteryWorkerFactory(private val repository: BatteryRepository) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            BatteryMonitorWorker::class.java.name -> BatteryMonitorWorker(appContext, workerParameters, repository)
            else -> null
        }
    }
}
