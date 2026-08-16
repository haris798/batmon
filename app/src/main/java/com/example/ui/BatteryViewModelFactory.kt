package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.BatteryRepository
import com.example.domain.BatteryUsageEstimator

class BatteryViewModelFactory(
    private val repository: BatteryRepository,
    private val estimator: BatteryUsageEstimator
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BatteryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BatteryViewModel(repository, estimator) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
