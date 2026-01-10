package com.example.neokratos.ui.screen.bodymetrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.entity.BodyMetricEntity
import com.example.neokratos.data.local.entity.BodyMetricType
import com.example.neokratos.data.repository.BodyMetricTimeRange
import com.example.neokratos.data.repository.BodyMetricsRepository
import com.example.neokratos.data.repository.WeightStats
import com.example.neokratos.data.repository.WeightTrend
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Body Metrics screen.
 *
 * Manages:
 * - Weight tracking and stats
 * - Body measurements
 * - Progress photos
 * - Time range filtering
 */
class BodyMetricsViewModel(
    private val bodyMetricsRepository: BodyMetricsRepository
) : ViewModel() {

    // ===== STATE =====

    /**
     * Weight statistics.
     */
    private val _weightStats = MutableStateFlow<WeightStats?>(null)
    val weightStats: StateFlow<WeightStats?> = _weightStats.asStateFlow()

    /**
     * Weight trend direction.
     */
    private val _weightTrend = MutableStateFlow<WeightTrend?>(null)
    val weightTrend: StateFlow<WeightTrend?> = _weightTrend.asStateFlow()

    /**
     * Selected time range for filtering.
     */
    private val _selectedTimeRange = MutableStateFlow(BodyMetricTimeRange.LAST_30_DAYS)
    val selectedTimeRange: StateFlow<BodyMetricTimeRange> = _selectedTimeRange.asStateFlow()

    /**
     * Weight history for selected time range.
     */
    private val _weightHistory = MutableStateFlow<List<BodyMetricEntity>>(emptyList())
    val weightHistory: StateFlow<List<BodyMetricEntity>> = _weightHistory.asStateFlow()

    /**
     * All measurements (non-weight, non-photo).
     */
    val allMeasurements: StateFlow<List<BodyMetricEntity>> =
        bodyMetricsRepository.allMetrics
            .map { metrics ->
                metrics.filter { metric ->
                    metric.type != BodyMetricType.WEIGHT &&
                            !BodyMetricType.isPhotoType(metric.type)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * All photos.
     */
    val allPhotos: StateFlow<List<BodyMetricEntity>> =
        bodyMetricsRepository.allPhotos
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // ===== INITIALIZATION =====

    init {
        loadWeightStats()
        loadWeightTrend()
        loadWeightHistory()
    }

    // ===== ACTIONS =====

    /**
     * Log a weight measurement.
     */
    fun logWeight(weightKg: Float, notes: String? = null) {
        viewModelScope.launch {
            try {
                bodyMetricsRepository.logWeight(weightKg, notes)
                // Reload stats and history
                loadWeightStats()
                loadWeightTrend()
                loadWeightHistory()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Log a body measurement.
     */
    fun logMeasurement(type: BodyMetricType, valueCm: Float, notes: String? = null) {
        viewModelScope.launch {
            try {
                bodyMetricsRepository.logMeasurement(type, valueCm, notes)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Delete a metric.
     */
    fun deleteMetric(metricId: Long) {
        viewModelScope.launch {
            try {
                bodyMetricsRepository.deleteMetric(metricId)
                // Reload stats if it was a weight
                loadWeightStats()
                loadWeightTrend()
                loadWeightHistory()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    /**
     * Select a time range.
     * Triggers reload of weight history.
     */
    fun selectTimeRange(range: BodyMetricTimeRange) {
        _selectedTimeRange.value = range
        loadWeightHistory()
    }

    /**
     * Refresh all data.
     */
    fun refresh() {
        loadWeightStats()
        loadWeightTrend()
        loadWeightHistory()
    }

    // ===== DATA LOADING =====

    /**
     * Load weight statistics.
     */
    private fun loadWeightStats() {
        viewModelScope.launch {
            try {
                val stats = bodyMetricsRepository.getWeightStats()
                _weightStats.value = stats
            } catch (e: Exception) {
                _weightStats.value = null
            }
        }
    }

    /**
     * Load weight trend direction.
     */
    private fun loadWeightTrend() {
        viewModelScope.launch {
            try {
                val trend = bodyMetricsRepository.getWeightTrendDirection()
                _weightTrend.value = trend
            } catch (e: Exception) {
                _weightTrend.value = null
            }
        }
    }

    /**
     * Load weight history for selected time range.
     */
    private fun loadWeightHistory() {
        viewModelScope.launch {
            try {
                val history = bodyMetricsRepository.getWeightGraphData(_selectedTimeRange.value)
                _weightHistory.value = history
            } catch (e: Exception) {
                _weightHistory.value = emptyList()
            }
        }
    }
}