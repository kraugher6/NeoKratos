package com.example.neokratos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.neokratos.data.local.entity.WorkoutSessionEntity
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.local.relations.SessionWithExercises
import kotlinx.coroutines.flow.Flow

/**
 * DAO for WorkoutSession operations.
 */
@Dao
interface WorkoutSessionDao {

    // ===== INSERT / UPDATE / DELETE =====

    /**
     * Insert a new workout session.
     * Returns the ID of the inserted session.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutSessionEntity): Long

    /**
     * Update a workout session.
     * Used for updating endTime, volume, notes, etc.
     */
    @Update
    suspend fun update(workout: WorkoutSessionEntity)

    /**
     * Delete a workout session.
     * Cascade will also delete all exercises and sets.
     */
    @Delete
    suspend fun delete(workout: WorkoutSessionEntity)

    /**
     * Delete session by ID.
     */
    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: Long)

    // ===== BASIC QUERIES =====

    /**
     * Get all workout sessions (basic info only, no exercises).
     * Ordered by date (newest first).
     */
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>>

    /**
     * Get workout session by ID.
     */
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: Long): WorkoutSessionEntity?

    /**
     * Get workout session by ID as Flow.
     */
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun getByIdFlow(sessionId: Long): Flow<WorkoutSessionEntity?>

    /**
     * Get active (in-progress) workout session.
     * Returns the session with endTime = null.
     *
     * Should only be one active session at a time.
     */
    @Query("SELECT * FROM workout_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveWorkout(): WorkoutSessionEntity?

    /**
     * Get active workout as Flow (reactive).
     * UI observes this to detect when workout starts/ends.
     */
    @Query("SELECT * FROM workout_sessions WHERE endTime IS NULL LIMIT 1")
    fun getActiveWorkoutFlow(): Flow<WorkoutSessionEntity?>

    /**
     * Get completed workouts only.
     */
    @Query("SELECT * FROM workout_sessions WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun getCompletedWorkouts(): Flow<List<WorkoutSessionEntity>>

    // ===== QUERIES WITH RELATIONS =====

    /**
     * Get session WITH its exercises (without set details).
     */
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionWithExercises(sessionId: Long): SessionWithExercises?

    /**
     * Get session WITH exercises as Flow.
     */
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun getSessionWithExercisesFlow(sessionId: Long): Flow<SessionWithExercises?>

    /**
     * Get complete session (exercises + sets + exercise details).
     *
     * THIS IS THE MAIN QUERY FOR ACTIVE WORKOUT SCREEN.
     * Returns everything needed to display workout.
     */
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionComplete(sessionId: Long): SessionComplete?

    /**
     * Get complete session as Flow (reactive).
     *
     * USE THIS FOR ACTIVE WORKOUT SCREEN.
     * UI updates automatically when sets are added.
     */
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun getSessionCompleteFlow(sessionId: Long): Flow<SessionComplete?>

    /**
     * Get active workout complete (for active workout screen).
     */
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE endTime IS NULL LIMIT 1")
    fun getActiveWorkoutComplete(): Flow<SessionComplete?>

    /**
     * Get all completed sessions with exercises.
     * Used for history list showing exercise count.
     */
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun getAllCompletedWithExercises(): Flow<List<SessionWithExercises>>

    /**
     * Get all completed sessions with full details.
     * Use this for history screen with volume/RPE display.
     */
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun getAllCompletedComplete(): Flow<List<SessionComplete>>

    // ===== TEMPLATE-BASED QUERIES =====

    /**
     * Get all sessions based on a specific template.
     * Useful for "Previous workouts from this template".
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE templateId = :templateId AND endTime IS NOT NULL
        ORDER BY startTime DESC
    """)
    fun getSessionsForTemplate(templateId: Long): Flow<List<WorkoutSessionEntity>>

    /**
     * Get most recent session for a template.
     * Used for "Last time you did this template" feature.
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE templateId = :templateId AND endTime IS NOT NULL
        ORDER BY startTime DESC 
        LIMIT 1
    """)
    suspend fun getMostRecentSessionForTemplate(templateId: Long): WorkoutSessionEntity?

    /**
     * Get most recent complete session for a template.
     */
    @Transaction
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE templateId = :templateId AND endTime IS NOT NULL
        ORDER BY startTime DESC 
        LIMIT 1
    """)
    suspend fun getMostRecentCompleteForTemplate(templateId: Long): SessionComplete?

    // ===== TIME-BASED QUERIES =====

    /**
     * Get sessions in a date range.
     * Used for analytics (this week, this month, etc.)
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE startTime >= :startTime AND startTime <= :endTime
        ORDER BY startTime DESC
    """)
    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<WorkoutSessionEntity>>

    /**
     * Get sessions for a specific date (day).
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE startTime >= :dayStart AND startTime < :dayEnd
        ORDER BY startTime DESC
    """)
    fun getSessionsForDay(dayStart: Long, dayEnd: Long): Flow<List<WorkoutSessionEntity>>

    // ===== STATS =====

    /**
     * Get total number of completed workouts.
     */
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE endTime IS NOT NULL")
    suspend fun getCompletedCount(): Int

    /**
     * Get total number of workouts (including in-progress).
     */
    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun getTotalCount(): Int

    /**
     * Get total volume across all workouts.
     */
    @Query("SELECT SUM(totalVolume) FROM workout_sessions WHERE endTime IS NOT NULL")
    suspend fun getTotalVolumeAllTime(): Float?

    /**
     * Get average workout duration in seconds.
     */
    @Query("""
        SELECT AVG((endTime - startTime) / 1000) 
        FROM workout_sessions 
        WHERE endTime IS NOT NULL
    """)
    suspend fun getAverageDuration(): Long?

    /**
     * Check if there's an active workout.
     */
    @Query("SELECT COUNT(*) > 0 FROM workout_sessions WHERE endTime IS NULL")
    suspend fun hasActiveWorkout(): Boolean

    // ===== UPDATE HELPERS =====

    /**
     * Mark session as completed.
     * Sets endTime and calculates final stats.
     */
    @Query("""
        UPDATE workout_sessions 
        SET endTime = :endTime,
            totalVolume = :totalVolume,
            totalSets = :totalSets,
            averageRPE = :averageRPE
        WHERE id = :sessionId
    """)
    suspend fun completeWorkout(
        sessionId: Long,
        endTime: Long,
        totalVolume: Float,
        totalSets: Int,
        averageRPE: Float?
    )

    /**
     * Update session notes.
     */
    @Query("UPDATE workout_sessions SET notes = :notes WHERE id = :sessionId")
    suspend fun updateNotes(sessionId: Long, notes: String?)

    /**
     * Update session name.
     */
    @Query("UPDATE workout_sessions SET name = :name WHERE id = :sessionId")
    suspend fun updateName(sessionId: Long, name: String?)

    /**
     * Update bodyweight for session.
     */
    @Query("UPDATE workout_sessions SET bodyweight = :bodyweight WHERE id = :sessionId")
    suspend fun updateBodyweight(sessionId: Long, bodyweight: Float?)
}