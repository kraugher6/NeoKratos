package com.example.neokratos.data.repository

import com.example.neokratos.data.local.dao.SetLogDao
import com.example.neokratos.data.local.dao.SessionExerciseDao
import com.example.neokratos.data.local.dao.WorkoutSessionDao
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.entity.WorkoutSessionEntity
import com.example.neokratos.data.local.entity.estimateOneRepMax
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Repository for analytics and statistics.
 *
 * Aggregates data from workouts, exercises, and sets to provide:
 * - Exercise progress (weight, volume, 1RM over time)
 * - Workout stats (frequency, duration, volume)
 * - Personal records
 * - Muscle group distribution
 *
 * Concepts:
 * - Data aggregation: combining multiple data sources
 * - Time-based queries: filtering by date ranges
 * - Statistical calculations: max, avg, trends
 */
class AnalyticsRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val sessionExerciseDao: SessionExerciseDao,
    private val setLogDao: SetLogDao
) {

    // ===== EXERCISE ANALYTICS =====

    /**
     * Get all sets for an exercise over time.
     * Used for progress graphs.
     */
    fun getSetsForExercise(exerciseId: Long): Flow<List<SetLogEntity>> {
        return setLogDao.getAllSetsForExercise(exerciseId)
    }

    /**
     * Get sets for an exercise in a time range.
     *
     * @param exerciseId Exercise to analyze
     * @param startTime Start of range (millis)
     * @param endTime End of range (millis)
     */
    suspend fun getSetsInTimeRange(
        exerciseId: Long,
        startTime: Long,
        endTime: Long
    ): List<SetLogEntity> {
        return setLogDao.getSetsInTimeRange(exerciseId, startTime, endTime)
    }

    /**
     * Get exercise progress data points.
     * Each point contains: date, max weight, max volume, estimated 1RM.
     *
     * Groups sets by workout session for cleaner visualization.
     */
    suspend fun getExerciseProgressData(
        exerciseId: Long,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<ExerciseProgressPoint> {
        // Get all sets in range
        val sets = if (startTime != null && endTime != null) {
            getSetsInTimeRange(exerciseId, startTime, endTime)
        } else {
            // Get all sets
            val allSets = mutableListOf<SetLogEntity>()
            getSetsForExercise(exerciseId).collect { allSets.addAll(it) }
            allSets
        }

        // Group sets by session (via sessionExerciseId)
        val sessionGroups = sets.groupBy { it.sessionExerciseId }

        // For each session, calculate stats
        return sessionGroups.map { (sessionExerciseId, sessionSets) ->
            val sessionExercise = sessionExerciseDao.getById(sessionExerciseId)
            val timestamp = sessionExercise?.startTime ?: sessionSets.first().timestamp

            ExerciseProgressPoint(
                timestamp = timestamp,
                maxWeight = sessionSets.maxOfOrNull { it.weight } ?: 0f,
                totalVolume = sessionSets.sumOf { (it.weight * it.reps).toDouble() }.toFloat(),
                maxEstimated1RM = sessionSets.maxOfOrNull { it.estimateOneRepMax() } ?: 0f,
                totalSets = sessionSets.size,
                avgRPE = sessionSets.mapNotNull { it.rpe }.average().toFloat()
            )
        }.sortedBy { it.timestamp }
    }

    /**
     * Get personal record (best set) for an exercise.
     */
    suspend fun getPersonalRecord(exerciseId: Long): SetLogEntity? {
        return setLogDao.getBestSetForExercise(exerciseId)
    }

    /**
     * Get top working sets for an exercise.
     * Sorted by weight × reps (volume per set).
     */
    suspend fun getTopWorkingSets(exerciseId: Long, limit: Int = 10): List<SetLogEntity> {
        return setLogDao.getTopWorkingSets(exerciseId, limit)
    }

    // ===== WORKOUT STATISTICS =====

    /**
     * Get workout frequency by time range.
     * Returns number of workouts per week/month.
     */
    suspend fun getWorkoutFrequency(rangeType: TimeRange): FrequencyStats {
        val (startTime, endTime) = getTimeRangeBounds(rangeType)

        val workouts = mutableListOf<WorkoutSessionEntity>()
        workoutSessionDao.getSessionsInRange(startTime, endTime).collect { workouts.addAll(it) }

        val days = TimeUnit.MILLISECONDS.toDays(endTime - startTime)
        val weeks = days / 7.0

        return FrequencyStats(
            totalWorkouts = workouts.size,
            averagePerWeek = if (weeks > 0) workouts.size / weeks else 0.0,
            rangeStart = startTime,
            rangeEnd = endTime
        )
    }

    /**
     * Get total volume over time.
     * Groups by week or month for visualization.
     */
    suspend fun getVolumeOverTime(
        groupBy: TimeGrouping,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<VolumeDataPoint> {
        val (start, end) = if (startTime != null && endTime != null) {
            startTime to endTime
        } else {
            getTimeRangeBounds(TimeRange.LAST_3_MONTHS)
        }

        val workouts = mutableListOf<WorkoutSessionEntity>()
        workoutSessionDao.getSessionsInRange(start, end).collect { workouts.addAll(it) }

        // Group workouts by time period
        return when (groupBy) {
            TimeGrouping.WEEK -> groupByWeek(workouts)
            TimeGrouping.MONTH -> groupByMonth(workouts)
        }
    }

    /**
     * Get average workout duration.
     */
    suspend fun getAverageWorkoutDuration(): Long? {
        return workoutSessionDao.getAverageDuration()
    }

    /**
     * Get total lifetime stats.
     */
    suspend fun getLifetimeStats(): LifetimeStats {
        val totalWorkouts = workoutSessionDao.getCompletedCount()
        val totalVolume = workoutSessionDao.getTotalVolumeAllTime() ?: 0f
        val avgDuration = workoutSessionDao.getAverageDuration() ?: 0L

        return LifetimeStats(
            totalWorkouts = totalWorkouts,
            totalVolume = totalVolume,
            averageDuration = avgDuration
        )
    }

    // ===== MUSCLE GROUP DISTRIBUTION =====

    /**
     * Calculate muscle group distribution for a time range.
     * Shows which muscle groups were trained most.
     */
    suspend fun getMuscleGroupDistribution(
        startTime: Long,
        endTime: Long
    ): Map<String, Int> {
        // This requires joining sessions -> exercises -> exercise details
        // For now, return placeholder
        // TODO: Implement proper query joining all tables
        return emptyMap()
    }

    // ===== HELPER FUNCTIONS =====

    /**
     * Get time range bounds in milliseconds.
     */
    private fun getTimeRangeBounds(range: TimeRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.apply {
            when (range) {
                TimeRange.LAST_7_DAYS -> add(Calendar.DAY_OF_YEAR, -7)
                TimeRange.LAST_30_DAYS -> add(Calendar.DAY_OF_YEAR, -30)
                TimeRange.LAST_3_MONTHS -> add(Calendar.MONTH, -3)
                TimeRange.LAST_6_MONTHS -> add(Calendar.MONTH, -6)
                TimeRange.LAST_YEAR -> add(Calendar.YEAR, -1)
                TimeRange.ALL_TIME -> timeInMillis = 0
            }
        }

        return calendar.timeInMillis to endTime
    }

    /**
     * Group workouts by week.
     */
    private fun groupByWeek(workouts: List<WorkoutSessionEntity>): List<VolumeDataPoint> {
        val calendar = Calendar.getInstance()

        return workouts
            .groupBy { workout ->
                calendar.timeInMillis = workout.startTime
                calendar.get(Calendar.WEEK_OF_YEAR) to calendar.get(Calendar.YEAR)
            }
            .map { (weekYear, workoutsInWeek) ->
                VolumeDataPoint(
                    timestamp = workoutsInWeek.first().startTime,
                    volume = workoutsInWeek.sumOf { it.totalVolume.toDouble() }.toFloat(),
                    workoutCount = workoutsInWeek.size
                )
            }
            .sortedBy { it.timestamp }
    }

    /**
     * Group workouts by month.
     */
    private fun groupByMonth(workouts: List<WorkoutSessionEntity>): List<VolumeDataPoint> {
        val calendar = Calendar.getInstance()

        return workouts
            .groupBy { workout ->
                calendar.timeInMillis = workout.startTime
                calendar.get(Calendar.MONTH) to calendar.get(Calendar.YEAR)
            }
            .map { (monthYear, workoutsInMonth) ->
                VolumeDataPoint(
                    timestamp = workoutsInMonth.first().startTime,
                    volume = workoutsInMonth.sumOf { it.totalVolume.toDouble() }.toFloat(),
                    workoutCount = workoutsInMonth.size
                )
            }
            .sortedBy { it.timestamp }
    }
}

// ===== DATA CLASSES =====

/**
 * Single data point for exercise progress graph.
 */
data class ExerciseProgressPoint(
    val timestamp: Long,
    val maxWeight: Float,
    val totalVolume: Float,
    val maxEstimated1RM: Float,
    val totalSets: Int,
    val avgRPE: Float
)

/**
 * Workout frequency statistics.
 */
data class FrequencyStats(
    val totalWorkouts: Int,
    val averagePerWeek: Double,
    val rangeStart: Long,
    val rangeEnd: Long
)

/**
 * Volume data point for time-series graphs.
 */
data class VolumeDataPoint(
    val timestamp: Long,
    val volume: Float,
    val workoutCount: Int
)

/**
 * Lifetime statistics.
 */
data class LifetimeStats(
    val totalWorkouts: Int,
    val totalVolume: Float,
    val averageDuration: Long // in seconds
)

/**
 * Time range options for queries.
 */
enum class TimeRange {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    LAST_YEAR,
    ALL_TIME
}

/**
 * Time grouping for aggregations.
 */
enum class TimeGrouping {
    WEEK,
    MONTH
}