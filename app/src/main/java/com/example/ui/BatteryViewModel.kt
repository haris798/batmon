package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BatterySampleEntity
import com.example.data.local.DischargeSessionEntity
import com.example.data.repository.BatteryRepository
import com.example.domain.BatteryUsageEstimator
import com.example.domain.UsageStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BatteryUiState(
    val latestSample: BatterySampleEntity? = null,
    val activeSession: DischargeSessionEntity? = null,
    val usageStats: UsageStats? = null,
    val isLoading: Boolean = true
)

class BatteryViewModel(
    private val repository: BatteryRepository,
    private val estimator: BatteryUsageEstimator
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _usageStats = MutableStateFlow<UsageStats?>(null)

    val uiState: StateFlow<BatteryUiState> = combine(
        repository.getLatestSample(),
        repository.getLatestSessionFlow(),
        _usageStats,
        _isLoading
    ) { sample, session, stats, loading ->
        BatteryUiState(
            latestSample = sample,
            activeSession = session,
            usageStats = stats,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BatteryUiState()
    )

    init {
        // Start foreground sampling loop for baseline heartbeat
        viewModelScope.launch {
            while (true) {
                repository.recordSample()
                delay(60_000) // Heartbeat every 60 seconds
            }
        }
        
        // Reactively update stats whenever a new sample is recorded
        viewModelScope.launch {
            repository.getLatestSample().collect {
                updateStats()
            }
        }
    }

    private suspend fun updateStats() {
        // Get samples for the current session or last 24h if no session
        val latestSession = repository.getLatestSessionFlow().stateIn(viewModelScope, SharingStarted.Eagerly, null).value
        
        val since = latestSession?.startTime ?: (System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        
        val samples = repository.getSamplesSince(since)
        val stats = estimator.estimateUsage(samples)
        _usageStats.value = stats
        _isLoading.value = false
    }
}
