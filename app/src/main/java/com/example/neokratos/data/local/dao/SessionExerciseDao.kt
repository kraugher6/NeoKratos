package com.example.neokratos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.neokratos.data.local.entity.SessionExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for SessionExercise operations.
 *
 * Manages exercises within workout sessions.
 */
@Dao
interface SessionExerciseDao {

    // ===== INSERT / UPDATE / DELETE =====

    /**
     * Insert an exercise into a session.
     * Returns the ID of the inserted row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sessionExercise: SessionExerciseEntity): Long

    /**
     * Insert multiple exercises at once.
     * Useful when starting workout from template.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessionExercises: List<SessionExerciseEntity>): List<Long>

    /**
     * Update a session exercise.
     * Used for updating notes, completion time, etc.
     */
    @Update
    suspend fun update(sessionExercise: SessionExerciseEntity)

    /**
     * Delete a session exercise.
     * Cascade will also delete all its sets.
     */
    @Delete
    suspend fun delete(sessionExercise: SessionExerciseEntity)

    /**
     * Delete session exercise by ID.
     */
    @Query("DELETE FROM session_exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Delete all exercises from a session.
     * Used when clearing/canceling a workout.
     */
    @Query("DELETE FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun deleteAllForSession(sessionId: Long)

    // ===== BASIC QUERIES =====

    /**
     * Get all exercises for a session, ordered by position.
     * Returns Flow for reactive UI updates during workout.
     */
    @Query("""
        SELECT * FROM session_exercises 
        WHERE sessionId = :sessionId 
        ORDER BY `order` ASC
    """)
    fun getForSession(sessionId: Long): Flow<List<SessionExerciseEntity>>

    /**
     * Get all exercises for a session (one-shot, not reactive).
     */
    @Query("""
        SELECT * FROM session_exercises 
        WHERE sessionId = :sessionId 
        ORDER BY `order` ASC
    """)
    suspend fun getForSessionOneShot(sessionId: Long): List<SessionExerciseEntity>

    /**
     * Get session exercise by ID.
     */
    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun getById(id: Long): SessionExerciseEntity?

    /**
     * Get session exercise by ID as Flow.
     */
    @Query("SELECT * FROM session_exercises WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<SessionExerciseEntity?>

    /**
     * Get all session exercises for a specific exercise (across all sessions).
     * Useful for "last time I did this exercise" feature.
     */
    @Query("""
        SELECT * FROM session_exercises 
        WHERE exerciseId = :exerciseId 
        ORDER BY startTime DESC
    """)
    fun getHistoryForExercise(exerciseId: Long): Flow<List<SessionExerciseEntity>>

    /**
     * Get most recent session exercise for a specific exercise.
     * Used for "previous workout" comparison.
     */
    @Query("""
        SELECT * FROM session_exercises 
        WHERE exerciseId = :exerciseId 
        ORDER BY startTime DESC 
        LIMIT 1
    """)
    suspend fun getMostRecentForExercise(exerciseId: Long): SessionExerciseEntity?

    // ===== COMPLETION TRACKING =====

    /**
     * Mark exercise as completed.
     * Sets endTime to current time.
     */
    @Query("UPDATE session_exercises SET endTime = :endTime WHERE id = :id")
    suspend fun markCompleted(id: Long, endTime: Long = System.currentTimeMillis())

    /**
     * Check if exercise is completed.
     */
    @Query("SELECT endTime IS NOT NULL FROM session_exercises WHERE id = :id")
    suspend fun isCompleted(id: Long): Boolean

    /**
     * Get count of completed exercises in session.
     */
    @Query("""
        SELECT COUNT(*) FROM session_exercises 
        WHERE sessionId = :sessionId AND endTime IS NOT NULL
    """)
    suspend fun getCompletedCount(sessionId: Long): Int

    /**
     * Get count of total exercises in session.
     */
    @Query("SELECT COUNT(*) FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun getTotalCount(sessionId: Long): Int

    // ===== REORDERING =====

    /**
     * Update order for a specific exercise.
     * Used during drag-and-drop reordering in active workout.
     */
    @Query("UPDATE session_exercises SET `order` = :newOrder WHERE id = :id")
    suspend fun updateOrder(id: Long, newOrder: Int)

    /**
     * Batch update orders for multiple exercises.
     */
    @Transaction
    suspend fun reorderExercises(updates: List<Pair<Long, Int>>) {
        updates.forEach { (id, order) ->
            updateOrder(id, order)
        }
    }

    // ===== NOTES =====

    /**
     * Update notes for a session exercise.
     */
    @Query("UPDATE session_exercises SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String?)

    // ===== STATS =====

    /**
     * Get total number of times an exercise has been performed (across all sessions).
     * Useful for "You've done this 23 times" display.
     */
    @Query("SELECT COUNT(*) FROM session_exercises WHERE exerciseId = :exerciseId")
    suspend fun getTimesPerformed(exerciseId: Long): Int
}