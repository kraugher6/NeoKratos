package com.example.neokratos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.neokratos.data.local.entity.BodyMetricEntity
import com.example.neokratos.data.local.entity.BodyMetricType
import kotlinx.coroutines.flow.Flow

/**
 * DAO for BodyMetric operations.
 *
 * Handles:
 * - Logging weight and measurements
 * - Retrieving metrics over time
 * - Progress photos
 * - Time-series queries
 */
@Dao
interface BodyMetricDao {

    // ===== INSERT / UPDATE / DELETE =====

    /**
     * Insert a body metric.
     * Returns the ID of the inserted row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: BodyMetricEntity): Long

    /**
     * Insert multiple metrics at once.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metrics: List<BodyMetricEntity>): List<Long>

    /**
     * Update a metric.
     */
    @Update
    suspend fun update(metric: BodyMetricEntity)

    /**
     * Delete a metric.
     */
    @Delete
    suspend fun delete(metric: BodyMetricEntity)

    /**
     * Delete metric by ID.
     */
    @Query("DELETE FROM body_metrics WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ===== BASIC QUERIES =====

    /**
     * Get all metrics, ordered by timestamp (newest first).
     */
    @Query("SELECT * FROM body_metrics ORDER BY timestamp DESC")
    fun getAllMetrics(): Flow<List<BodyMetricEntity>>

    /**
     * Get metric by ID.
     */
    @Query("SELECT * FROM body_metrics WHERE id = :id")
    suspend fun getById(id: Long): BodyMetricEntity?

    // ===== TYPE-SPECIFIC QUERIES =====

    /**
     * Get all metrics of a specific type.
     * Example: Get all weight measurements.
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE type = :type 
        ORDER BY timestamp DESC
    """)
    fun getByType(type: BodyMetricType): Flow<List<BodyMetricEntity>>

    /**
     * Get most recent metric of a type.
     * Example: Get latest weight.
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE type = :type 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getMostRecentByType(type: BodyMetricType): BodyMetricEntity?

    /**
     * Get most recent metric of a type as Flow.
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE type = :type 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    fun getMostRecentByTypeFlow(type: BodyMetricType): Flow<BodyMetricEntity?>

    // ===== WEIGHT-SPECIFIC QUERIES =====

    /**
     * Get all weight measurements.
     * Returns Flow for reactive UI.
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE type = 'WEIGHT' 
        ORDER BY timestamp DESC
    """)
    fun getAllWeights(): Flow<List<BodyMetricEntity>>

    /**
     * Get current (most recent) weight.
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE type = 'WEIGHT' 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getCurrentWeight(): BodyMetricEntity?

    /**
     * Get weight in time range.
     * Used for weight graphs (last 30 days, 3 months, etc.)
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE type = 'WEIGHT' 
          AND timestamp >= :startTime 
          AND timestamp <= :endTime
        ORDER BY timestamp ASC
    """)
    suspend fun getWeightsInRange(startTime: Long, endTime: Long): List<BodyMetricEntity>

    // ===== TIME-BASED QUERIES =====

    /**
     * Get metrics in a time range.
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE timestamp >= :startTime 
          AND timestamp <= :endTime
        ORDER BY timestamp DESC
    """)
    fun getMetricsInRange(startTime: Long, endTime: Long): Flow<List<BodyMetricEntity>>

    /**
     * Get metrics by type in time range.
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE type = :type 
          AND timestamp >= :startTime 
          AND timestamp <= :endTime
        ORDER BY timestamp ASC
    """)
    suspend fun getByTypeInRange(
        type: BodyMetricType,
        startTime: Long,
        endTime: Long
    ): List<BodyMetricEntity>

    // ===== PHOTO QUERIES =====

    /**
     * Get all progress photos.
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE type IN ('PHOTO_FRONT', 'PHOTO_SIDE', 'PHOTO_BACK')
        ORDER BY timestamp DESC
    """)
    fun getAllPhotos(): Flow<List<BodyMetricEntity>>

    /**
     * Get photos in time range.
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE type IN ('PHOTO_FRONT', 'PHOTO_SIDE', 'PHOTO_BACK')
          AND timestamp >= :startTime 
          AND timestamp <= :endTime
        ORDER BY timestamp DESC
    """)
    fun getPhotosInRange(startTime: Long, endTime: Long): Flow<List<BodyMetricEntity>>

    // ===== STATISTICS =====

    /**
     * Get count of metrics.
     */
    @Query("SELECT COUNT(*) FROM body_metrics")
    suspend fun getCount(): Int

    /**
     * Get count by type.
     */
    @Query("SELECT COUNT(*) FROM body_metrics WHERE type = :type")
    suspend fun getCountByType(type: BodyMetricType): Int

    /**
     * Get average value for a metric type.
     * Useful for "average weight last 30 days"
     */
    @Query("""
        SELECT AVG(value) FROM body_metrics 
        WHERE type = :type 
          AND timestamp >= :startTime 
          AND timestamp <= :endTime
    """)
    suspend fun getAverageInRange(
        type: BodyMetricType,
        startTime: Long,
        endTime: Long
    ): Float?

    /**
     * Get weight change (difference between first and last measurement).
     */
    @Query("""
        SELECT 
            (SELECT value FROM body_metrics WHERE type = 'WEIGHT' ORDER BY timestamp DESC LIMIT 1) -
            (SELECT value FROM body_metrics WHERE type = 'WEIGHT' ORDER BY timestamp ASC LIMIT 1)
        AS weight_change
    """)
    suspend fun getWeightChange(): Float?

    /**
     * Get weight trend (last N measurements).
     * Used for detecting gaining/losing weight.
     */
    @Query("""
        SELECT * FROM body_metrics 
        WHERE type = 'WEIGHT' 
        ORDER BY timestamp DESC 
        LIMIT :count
    """)
    suspend fun getRecentWeights(count: Int): List<BodyMetricEntity>
}