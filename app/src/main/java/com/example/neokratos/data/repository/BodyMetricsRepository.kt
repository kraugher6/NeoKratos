package com.example.neokratos.data.repository

import com.example.neokratos.data.local.dao.BodyMetricDao
import com.example.neokratos.data.local.entity.BodyMetricEntity
import com.example.neokratos.data.local.entity.BodyMetricType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Repository for body metrics operations.
 *
 * Manages:
 * - Weight tracking
 * - Body measurements
 * - Progress photos
 * - Trends and statistics
 */
class BodyMetricsRepository(
    private val bodyMetricDao: BodyMetricDao
) {

    // ===== EXPOSED FLOWS =====

    /**
     * All body metrics.
     */
    val allMetrics: Flow<List<BodyMetricEntity>> = bodyMetricDao.getAllMetrics()

    /**
     * All weight measurements.
     */
    val allWeights: Flow<List<BodyMetricEntity>> = bodyMetricDao.getAllWeights()

    /**
     * All progress photos.
     */
    val allPhotos: Flow<List<BodyMetricEntity>> = bodyMetricDao.getAllPhotos()

    /**
     * Current (most recent) weight as Flow.
     */
    fun getCurrentWeightFlow(): Flow<BodyMetricEntity?> {
        return bodyMetricDao.getMostRecentByTypeFlow(BodyMetricType.WEIGHT)
    }

    // ===== INSERT / UPDATE / DELETE =====

    /**
     * Log a body metric.
     * Returns the ID of the inserted metric.
     */
    suspend fun logMetric(metric: BodyMetricEntity): Long {
        return bodyMetricDao.insert(metric)
    }

    /**
     * Log weight measurement.
     * Convenience method for most common use case.
     */
    suspend fun logWeight(
        weightKg: Float,
        notes: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val metric = BodyMetricEntity(
            type = BodyMetricType.WEIGHT,
            value = weightKg,
            unit = "kg",
            timestamp = timestamp,
            notes = notes
        )
        return bodyMetricDao.insert(metric)
    }

    /**
     * Log body measurement.
     */
    suspend fun logMeasurement(
        type: BodyMetricType,
        valueCm: Float,
        notes: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        require(!BodyMetricType.isPhotoType(type)) {
            "Use logPhoto() for photo metrics"
        }
        require(type != BodyMetricType.WEIGHT) {
            "Use logWeight() for weight metrics"
        }

        val metric = BodyMetricEntity(
            type = type,
            value = valueCm,
            unit = "cm",
            timestamp = timestamp,
            notes = notes
        )
        return bodyMetricDao.insert(metric)
    }

    /**
     * Log progress photo.
     */
    suspend fun logPhoto(
        type: BodyMetricType,
        photoUri: String,
        notes: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        require(BodyMetricType.isPhotoType(type)) {
            "Type must be a photo type"
        }

        val metric = BodyMetricEntity(
            type = type,
            value = null,
            unit = null,
            timestamp = timestamp,
            notes = notes,
            photoUri = photoUri
        )
        return bodyMetricDao.insert(metric)
    }

    /**
     * Update a metric.
     */
    suspend fun updateMetric(metric: BodyMetricEntity) {
        bodyMetricDao.update(metric)
    }

    /**
     * Delete a metric.
     */
    suspend fun deleteMetric(metricId: Long) {
        bodyMetricDao.deleteById(metricId)
    }

    // ===== QUERIES =====

    /**
     * Get metrics by type.
     */
    fun getByType(type: BodyMetricType): Flow<List<BodyMetricEntity>> {
        return bodyMetricDao.getByType(type)
    }

    /**
     * Get most recent metric of a type.
     */
    suspend fun getMostRecent(type: BodyMetricType): BodyMetricEntity? {
        return bodyMetricDao.getMostRecentByType(type)
    }

    /**
     * Get current weight.
     */
    suspend fun getCurrentWeight(): BodyMetricEntity? {
        return bodyMetricDao.getCurrentWeight()
    }

    /**
     * Get weights in time range.
     */
    suspend fun getWeightsInRange(startTime: Long, endTime: Long): List<BodyMetricEntity> {
        return bodyMetricDao.getWeightsInRange(startTime, endTime)
    }

    /**
     * Get measurements by type in time range.
     */
    suspend fun getMeasurementsInRange(
        type: BodyMetricType,
        startTime: Long,
        endTime: Long
    ): List<BodyMetricEntity> {
        return bodyMetricDao.getByTypeInRange(type, startTime, endTime)
    }

    // ===== STATISTICS & TRENDS =====

    /**
     * Get weight statistics.
     */
    suspend fun getWeightStats(): WeightStats {
        val current = bodyMetricDao.getCurrentWeight()
        val change = bodyMetricDao.getWeightChange() ?: 0f
        val recent = bodyMetricDao.getRecentWeights(7)

        // Calculate 7-day trend
        val trend = if (recent.size >= 2) {
            val oldest = recent.last().value ?: 0f
            val newest = recent.first().value ?: 0f
            newest - oldest
        } else {
            0f
        }

        return WeightStats(
            currentWeight = current?.value,
            totalChange = change,
            sevenDayTrend = trend,
            measurementCount = bodyMetricDao.getCountByType(BodyMetricType.WEIGHT)
        )
    }

    /**
     * Get weight data for graph.
     * Returns data points for specified time range.
     */
    suspend fun getWeightGraphData(timeRange: BodyMetricTimeRange): List<BodyMetricEntity> {
        val (startTime, endTime) = getTimeRangeBounds(timeRange)
        return getWeightsInRange(startTime, endTime)
    }

    /**
     * Get average weight in time range.
     */
    suspend fun getAverageWeight(startTime: Long, endTime: Long): Float? {
        return bodyMetricDao.getAverageInRange(BodyMetricType.WEIGHT, startTime, endTime)
    }

    /**
     * Detect weight trend direction.
     */
    suspend fun getWeightTrendDirection(): WeightTrend {
        val recent = bodyMetricDao.getRecentWeights(7)

        if (recent.size < 2) return WeightTrend.STABLE

        val oldest = recent.last().value ?: return WeightTrend.STABLE
        val newest = recent.first().value ?: return WeightTrend.STABLE

        val change = newest - oldest

        return when {
            change > 0.5f -> WeightTrend.GAINING
            change < -0.5f -> WeightTrend.LOSING
            else -> WeightTrend.STABLE
        }
    }

    // ===== HELPER FUNCTIONS =====

    /**
     * Get time range bounds in milliseconds.
     */
    private fun getTimeRangeBounds(range: BodyMetricTimeRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.apply {
            when (range) {
                BodyMetricTimeRange.LAST_7_DAYS -> add(Calendar.DAY_OF_YEAR, -7)
                BodyMetricTimeRange.LAST_30_DAYS -> add(Calendar.DAY_OF_YEAR, -30)
                BodyMetricTimeRange.LAST_3_MONTHS -> add(Calendar.MONTH, -3)
                BodyMetricTimeRange.LAST_6_MONTHS -> add(Calendar.MONTH, -6)
                BodyMetricTimeRange.LAST_YEAR -> add(Calendar.YEAR, -1)
                BodyMetricTimeRange.ALL_TIME -> timeInMillis = 0
            }
        }

        return calendar.timeInMillis to endTime
    }
}

// ===== DATA CLASSES =====

/**
 * Weight statistics summary.
 */
data class WeightStats(
    val currentWeight: Float?,
    val totalChange: Float,
    val sevenDayTrend: Float,
    val measurementCount: Int
)

/**
 * Weight trend direction.
 */
enum class WeightTrend {
    GAINING,
    LOSING,
    STABLE
}

/**
 * Time range options for body metrics.
 */
enum class BodyMetricTimeRange {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    LAST_YEAR,
    ALL_TIME
}