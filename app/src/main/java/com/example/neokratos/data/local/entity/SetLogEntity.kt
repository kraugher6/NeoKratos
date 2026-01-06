package com.example.neokratos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single set performed during a workout.
 *
 * This is the most granular level of workout tracking.
 * Each set records: weight, reps, RPE, rest time, timestamp.
 *
 * Example:
 * Bench Press - Session Exercise:
 *   - Set 1: 100kg x 8 reps @ RPE 8, rest 90s
 *   - Set 2: 100kg x 7 reps @ RPE 9, rest 120s
 *   - Set 3: 95kg x 8 reps @ RPE 9, rest 0s (finished)
 *
 * Why track at set level?
 * - Precise progress tracking (compare set 1 vs set 1 across sessions)
 * - RPE-based auto-regulation (adjust weight based on RPE)
 * - Volume calculation (total tonnage)
 * - Rest time analysis
 */
@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE // Delete sets when exercise deleted
        )
    ],
    indices = [
        Index(value = ["sessionExerciseId"]),
        Index(value = ["timestamp"]) // For time-based queries
    ]
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Which exercise in the session this set belongs to.
     * References SessionExerciseEntity.id
     */
    val sessionExerciseId: Long,

    /**
     * Set number within the exercise.
     * 1 = first set, 2 = second set, etc.
     *
     * Important for tracking performance across sets:
     * "Is my set 3 weaker than set 1?"
     */
    val setNumber: Int,

    /**
     * Weight lifted in kg.
     *
     * For bodyweight exercises, use 0 or bodyweight value.
     * Float allows precision like 22.5kg plates.
     */
    val weight: Float,

    /**
     * Number of reps completed.
     *
     * For timed exercises (plank), use seconds as "reps"
     * or add a separate duration field later.
     */
    val reps: Int,

    /**
     * RPE (Rate of Perceived Exertion) on scale 1-10.
     *
     * RPE Scale:
     * - 10 = Maximum effort, couldn't do another rep
     * - 9.5 = Could maybe do 1 more rep
     * - 9 = Could do 1 more rep
     * - 8 = Could do 2-3 more reps
     * - 7 = Could do 3-4 more reps
     * - 6-1 = Progressively easier
     *
     * Critical for:
     * - Auto-regulation (adjust weight based on RPE)
     * - Preventing overtraining
     * - Progression algorithms
     *
     * Nullable because user might skip RPE entry.
     */
    val rpe: Float? = null,

    /**
     * Rest time AFTER this set (in seconds).
     *
     * Example: If you rest 90 seconds after set 1, this is 90.
     * Last set of exercise typically has restSeconds = 0 or null.
     *
     * Used for:
     * - Rest timer functionality
     * - Workout pace analysis
     * - Recovery patterns
     */
    val restSeconds: Int? = null,

    /**
     * Timestamp when this set was logged (milliseconds since epoch).
     *
     * Used for:
     * - Ordering sets by time (in case set numbers get messed up)
     * - Calculating actual rest time between sets
     * - Time-of-day performance analysis
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Whether this set was completed or skipped.
     *
     * False = set was planned but not done (injury, fatigue, time constraint)
     * True = set was performed
     *
     * Allows tracking "I planned 5 sets but only did 3"
     */
    val completed: Boolean = true,

    /**
     * Optional notes for this specific set.
     * Example: "Used spotter", "Felt tweaky", "Paused reps"
     *
     * Most notes go on SessionExercise or Session level.
     * This is for very specific set notes.
     */
    val notes: String? = null
)

/**
 * Extension functions for convenience and calculations.
 */

/**
 * Calculate volume for this set (weight x reps).
 * Example: 100kg x 8 = 800kg volume
 */
fun SetLogEntity.getVolume(): Float = weight * reps

/**
 * Estimate 1RM using Epley formula.
 * Formula: weight * (1 + reps/30)
 *
 * Most accurate for reps in 1-10 range.
 * Above 10 reps, becomes less accurate.
 */
fun SetLogEntity.estimateOneRepMax(): Float {
    if (reps == 0) return 0f
    return weight * (1 + reps / 30f)
}

/**
 * Estimate 1RM adjusted by RPE (more accurate).
 * Uses RPE to estimate how much more weight you could lift.
 *
 * RPE-adjusted formula:
 * - RPE 10 = 0 reps left in reserve (RIR)
 * - RPE 9 = 1 rep left
 * - RPE 8 = 2 reps left
 * etc.
 *
 * Example: 100kg x 8 @ RPE 8 means you could do 10 reps at that weight.
 * So we calculate 1RM from "100kg x 10" instead of "100kg x 8".
 */
fun SetLogEntity.estimateOneRepMaxWithRPE(): Float {
    if (reps == 0 || rpe == null) return estimateOneRepMax()

    // Convert RPE to reps in reserve (RIR)
    val rir = (10 - rpe).toInt()
    val effectiveReps = reps + rir

    // Use effective reps for 1RM calculation
    return weight * (1 + effectiveReps / 30f)
}

/**
 * Get rest time as display string.
 * Example: 90 seconds → "1:30"
 */
fun SetLogEntity.getRestTimeDisplay(): String {
    return restSeconds?.let {
        val minutes = it / 60
        val seconds = it % 60
        String.format("%d:%02d", minutes, seconds)
    } ?: "No rest"
}

/**
 * Get RPE as display string with decimal.
 * Example: 8.5 → "8.5", null → "N/A"
 */
fun SetLogEntity.getRPEDisplay(): String {
    return rpe?.let { String.format("%.1f", it) } ?: "N/A"
}

/**
 * Check if this is a heavy set (RPE >= 8.5).
 * Useful for progression algorithms.
 */
fun SetLogEntity.isHeavySet(): Boolean {
    return rpe?.let { it >= 8.5f } ?: false
}

/**
 * Check if this set has good form (RPE <= 8).
 * Sets with RPE > 8 might have form breakdown.
 */
fun SetLogEntity.hasGoodForm(): Boolean {
    return rpe?.let { it <= 8f } ?: true
}