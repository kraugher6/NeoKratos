package com.example.neokratos.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.repository.AnalyticsRepository
import com.example.neokratos.data.repository.FrequencyStats
import com.example.neokratos.data.repository.LifetimeStats
import com.example.neokratos.data.repository.TimeGrouping
import com.example.neokratos.data.repository.TimeRange
import com.example.neokratos.data.repository.VolumeDataPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Analytics screen.
 *
 * Manages:
 * - Lifetime statistics
 * - Time-range filtered statistics
 * - Volume trends over time
 * - Workout frequency
 *
 * Concepts:
 * - State management for multiple data streams
 * - Time range filtering
 * - Data aggregation coordination
 */
class AnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    // ===== STATE =====

    /**
     * Lifetime statistics (all-time).
     */
    private val _lifetimeStats = MutableStateFlow<LifetimeStats?>(null)
    val lifetimeStats: StateFlow<LifetimeStats?> = _lifetimeStats.asStateFlow()

    /**
     * Currently selected time range for filtering.
     */
    private val _selectedTimeRange = MutableStateFlow(TimeRange.LAST_30_DAYS)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    /**
     * Frequency statistics for selected time range.
     */
    private val _frequencyStats = MutableStateFlow<FrequencyStats?>(null)
    val frequencyStats: StateFlow<FrequencyStats?> = _frequencyStats.asStateFlow()

    /**
     * Volume over time data points.
     */
    private val _volumeOverTime = MutableStateFlow<List<VolumeDataPoint>>(emptyList())
    val volumeOverTime: StateFlow<List<VolumeDataPoint>> = _volumeOverTime.asStateFlow()

    // ===== INITIALIZATION =====

    init {
        loadLifetimeStats()
        loadFrequencyStats()
        loadVolumeOverTime()
    }

    // ===== ACTIONS =====

    /**
     * Select a new time range.
     * Triggers reload of filtered statistics.
     */
    fun selectTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        loadFrequencyStats()
        loadVolumeOverTime()
    }

    /**
     * Refresh all data.
     */
    fun refresh() {
        loadLifetimeStats()
        loadFrequencyStats()
        loadVolumeOverTime()
    }

    // ===== DATA LOADING =====

    /**
     * Load lifetime statistics.
     */
    private fun loadLifetimeStats() {
        viewModelScope.launch {
            try {
                val stats = analyticsRepository.getLifetimeStats()
                _lifetimeStats.value = stats
            } catch (e: Exception) {
                // Handle error
                _lifetimeStats.value = LifetimeStats(
                    totalWorkouts = 0,
                    totalVolume = 0f,
                    averageDuration = 0L
                )
            }
        }
    }

    /**
     * Load frequency statistics for selected time range.
     */
    private fun loadFrequencyStats() {
        viewModelScope.launch {
            try {
                val stats = analyticsRepository.getWorkoutFrequency(_selectedTimeRange.value)
                _frequencyStats.value = stats
            } catch (e: Exception) {
                // Handle error
                _frequencyStats.value = null
            }
        }
    }

    /**
     * Load volume over time data.
     */
    private fun loadVolumeOverTime() {
        viewModelScope.launch {
            try {
                val data = analyticsRepository.getVolumeOverTime(
                    groupBy = TimeGrouping.WEEK
                    // Uses current time range
                )
                _volumeOverTime.value = data
            } catch (e: Exception) {
                // Handle error
                _volumeOverTime.value = emptyList()
            }
        }
    }
}