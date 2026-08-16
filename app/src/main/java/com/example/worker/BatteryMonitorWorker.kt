package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.repository.BatteryRepository

class BatteryMonitorWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val batteryRepository: BatteryRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            batteryRepository.recordSample()
            batteryRepository.purgeOldSamples()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
