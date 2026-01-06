package com.example.neokratos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an exercise performed in a workout session.
 *
 * This is the junction between WorkoutSession and Exercise.
 * Each SessionExercise contains multiple sets (SetLogEntity).
 *
 * Example:
 * Session "Morning Workout - Jan 5":
 *   - SessionExercise: Bench Press (order: 0)
 *     - Set 1: 100kg x 8 @ RPE 8
 *     - Set 2: 100kg x 7 @ RPE 9
 *   - SessionExercise: Squat (order: 1)
 *     - Set 1: 140kg x 5 @ RPE 7
 *     ...
 *
 * Why separate SessionExercise from SetLog?
 * - Allows notes per exercise (not per set)
 * - Maintains order of exercises in session
 * - Clean separation: Session → Exercises → Sets
 */
@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE // Delete exercises when session deleted
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT // Can't delete exercise used in sessions
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"])
    ]
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Which workout session this exercise belongs to.
     */
    val sessionId: Long,

    /**
     * Which exercise from the library.
     * References ExerciseEntity.id
     */
    val exerciseId: Long,

    /**
     * Order/position in the session.
     * 0 = first exercise, 1 = second, etc.
     *
     * Allows reordering exercises during workout.
     */
    val order: Int,

    /**
     * Optional notes for this exercise in THIS session.
     * Example: "Felt strong today", "Left shoulder pain", "New PR!"
     *
     * Different from template notes (which are generic).
     * These notes are specific to this workout instance.
     */
    val notes: String? = null,

    /**
     * Timestamp when this exercise was started (milliseconds since epoch).
     * Useful for tracking workout pace and rest times.
     */
    val startTime: Long = System.currentTimeMillis(),

    /**
     * Timestamp when this exercise was completed (optional).
     * If null, exercise is still in progress.
     */
    val endTime: Long? = null
)

/**
 * Extension functions for convenience.
 */

/**
 * Check if exercise is completed.
 */
fun SessionExerciseEntity.isCompleted(): Boolean = endTime != null

/**
 * Get duration in seconds.
 * Returns null if not completed yet.
 */
fun SessionExerciseEntity.getDurationSeconds(): Long? {
    return endTime?.let { (it - startTime) / 1000 }
}

/**
 * Get duration as display string.
 * Example: "5:30" (5 minutes 30 seconds)
 */
fun SessionExerciseEntity.getDurationDisplay(): String {
    val seconds = getDurationSeconds() ?: return "In progress"
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}