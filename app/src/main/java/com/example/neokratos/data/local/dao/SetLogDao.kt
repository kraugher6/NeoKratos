package com.example.neokratos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.neokratos.data.local.entity.SetLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for SetLog operations.
 *
 * Manages individual sets within exercises.
 * This is the most granular level of workout tracking.
 */
@Dao
interface SetLogDao {

    // ===== INSERT / UPDATE / DELETE =====

    /**
     * Insert a set log.
     * Returns the ID of the inserted row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setLog: SetLogEntity): Long

    /**
     * Insert multiple sets at once.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(setLogs: List<SetLogEntity>): List<Long>

    /**
     * Update a set log.
     * Used for editing completed sets.
     */
    @Update
    suspend fun update(setLog: SetLogEntity)

    /**
     * Delete a set log.
     */
    @Delete
    suspend fun delete(setLog: SetLogEntity)

    /**
     * Delete set by ID.
     */
    @Query("DELETE FROM set_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Delete all sets for a session exercise.
     * Called when removing exercise from workout.
     */
    @Query("DELETE FROM set_logs WHERE sessionExerciseId = :sessionExerciseId")
    suspend fun deleteAllForSessionExercise(sessionExerciseId: Long)

    // ===== BASIC QUERIES =====

    /**
     * Get all sets for a session exercise, ordered by set number.
     * Returns Flow for reactive UI during workout.
     */
    @Query("""
        SELECT * FROM set_logs 
        WHERE sessionExerciseId = :sessionExerciseId 
        ORDER BY setNumber ASC
    """)
    fun getSetsForExercise(sessionExerciseId: Long): Flow<List<SetLogEntity>>

    /**
     * Get all sets for a session exercise (one-shot).
     */
    @Query("""
        SELECT * FROM set_logs 
        WHERE sessionExerciseId = :sessionExerciseId 
        ORDER BY setNumber ASC
    """)
    suspend fun getSetsForExerciseOneShot(sessionExerciseId: Long): List<SetLogEntity>

    /**
     * Get set by ID.
     */
    @Query("SELECT * FROM set_logs WHERE id = :id")
    suspend fun getById(id: Long): SetLogEntity?

    /**
     * Get set by ID as Flow.
     */
    @Query("SELECT * FROM set_logs WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<SetLogEntity?>

    // ===== PREVIOUS WORKOUT DATA =====

    /**
     * Get sets from previous workout for same exercise.
     *
     * Used for "Last time you did:" comparison during workout.
     * Shows user what they lifted last time to guide progression.
     *
     * Logic:
     * 1. Find previous SessionExercise for this exerciseId
     * 2. Get all sets from that session
     */
    @Query("""
        SELECT sl.* FROM set_logs sl
        INNER JOIN session_exercises se ON sl.sessionExerciseId = se.id
        WHERE se.exerciseId = :exerciseId 
          AND se.sessionId = :previousSessionId
        ORDER BY sl.setNumber ASC
    """)
    suspend fun getSetsFromPreviousWorkout(
        exerciseId: Long,
        previousSessionId: Long
    ): List<SetLogEntity>

    // ===== STATS & ANALYTICS =====

    /**
     * Get count of sets for a session exercise.
     */
    @Query("SELECT COUNT(*) FROM set_logs WHERE sessionExerciseId = :sessionExerciseId")
    suspend fun getSetCount(sessionExerciseId: Long): Int

    /**
     * Get count of completed sets (not skipped).
     */
    @Query("""
        SELECT COUNT(*) FROM set_logs 
        WHERE sessionExerciseId = :sessionExerciseId AND completed = 1
    """)
    suspend fun getCompletedSetCount(sessionExerciseId: Long): Int

    /**
     * Calculate total volume for a session exercise (sum of weight × reps).
     */
    @Query("""
        SELECT SUM(weight * reps) FROM set_logs 
        WHERE sessionExerciseId = :sessionExerciseId AND completed = 1
    """)
    suspend fun getTotalVolume(sessionExerciseId: Long): Float?

    /**
     * Get average RPE for a session exercise.
     */
    @Query("""
        SELECT AVG(rpe) FROM set_logs 
        WHERE sessionExerciseId = :sessionExerciseId 
          AND rpe IS NOT NULL 
          AND completed = 1
    """)
    suspend fun getAverageRPE(sessionExerciseId: Long): Float?

    /**
     * Get max weight lifted in a session exercise.
     * Useful for PR tracking.
     */
    @Query("""
        SELECT MAX(weight) FROM set_logs 
        WHERE sessionExerciseId = :sessionExerciseId AND completed = 1
    """)
    suspend fun getMaxWeight(sessionExerciseId: Long): Float?

    /**
     * Get set with highest estimated 1RM for an exercise (across all time).
     * Used for PR calculation.
     *
     * Estimated 1RM formula: weight * (1 + reps/30)
     */
    @Query("""
        SELECT * FROM set_logs sl
        INNER JOIN session_exercises se ON sl.sessionExerciseId = se.id
        WHERE se.exerciseId = :exerciseId AND sl.completed = 1
        ORDER BY (sl.weight * (1 + sl.reps / 30.0)) DESC
        LIMIT 1
    """)
    suspend fun getBestSetForExercise(exerciseId: Long): SetLogEntity?

    /**
     * Get all sets for an exercise across all sessions (for progress tracking).
     * Ordered by timestamp (newest first).
     */
    @Query("""
        SELECT sl.* FROM set_logs sl
        INNER JOIN session_exercises se ON sl.sessionExerciseId = se.id
        WHERE se.exerciseId = :exerciseId AND sl.completed = 1
        ORDER BY sl.timestamp DESC
    """)
    fun getAllSetsForExercise(exerciseId: Long): Flow<List<SetLogEntity>>

    /**
     * Get sets for an exercise in a specific time range.
     * Used for progress graphs (last 30 days, last 3 months, etc.)
     */
    @Query("""
        SELECT sl.* FROM set_logs sl
        INNER JOIN session_exercises se ON sl.sessionExerciseId = se.id
        WHERE se.exerciseId = :exerciseId 
          AND sl.timestamp >= :startTime 
          AND sl.timestamp <= :endTime
          AND sl.completed = 1
        ORDER BY sl.timestamp ASC
    """)
    suspend fun getSetsInTimeRange(
        exerciseId: Long,
        startTime: Long,
        endTime: Long
    ): List<SetLogEntity>

    /**
     * Get top working sets (heaviest weight × reps) for an exercise.
     * Used for progress tracking and PR display.
     * Limit to top N sets.
     */
    @Query("""
        SELECT sl.* FROM set_logs sl
        INNER JOIN session_exercises se ON sl.sessionExerciseId = se.id
        WHERE se.exerciseId = :exerciseId AND sl.completed = 1
        ORDER BY (sl.weight * sl.reps) DESC
        LIMIT :limit
    """)
    suspend fun getTopWorkingSets(exerciseId: Long, limit: Int = 10): List<SetLogEntity>

    // ===== RPE-BASED QUERIES (for auto-regulation) =====

    /**
     * Get average RPE for an exercise over last N sessions.
     * Used for auto-regulation: if avg RPE > 9, suggest deload.
     */
    @Query("""
        SELECT AVG(sl.rpe) FROM set_logs sl
        INNER JOIN session_exercises se ON sl.sessionExerciseId = se.id
        INNER JOIN workout_sessions ws ON se.sessionId = ws.id
        WHERE se.exerciseId = :exerciseId 
          AND sl.rpe IS NOT NULL 
          AND sl.completed = 1
        ORDER BY ws.startTime DESC
        LIMIT :sessionCount
    """)
    suspend fun getAverageRPELastNSessions(exerciseId: Long, sessionCount: Int): Float?

    /**
     * Get sets with RPE >= threshold (heavy sets).
     * Used for progression: "You hit RPE 9+ on 2 sets, time to increase weight"
     */
    @Query("""
        SELECT * FROM set_logs 
        WHERE sessionExerciseId = :sessionExerciseId 
          AND rpe >= :rpeThreshold 
          AND completed = 1
    """)
    suspend fun getHeavySets(sessionExerciseId: Long, rpeThreshold: Float = 8.5f): List<SetLogEntity>
}