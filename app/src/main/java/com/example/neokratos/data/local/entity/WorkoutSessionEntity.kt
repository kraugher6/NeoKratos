package com.example.neokratos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a completed or in-progress workout session.
 *
 * A session is a single workout instance:
 * - Started on a specific date/time
 * - May be based on a template (or freestyle)
 * - Contains multiple exercises
 * - Each exercise has multiple sets
 *
 * Example:
 * Session "Push Day - Jan 5, 2026":
 *   - Based on template "Push Day"
 *   - Started at 10:00 AM
 *   - Ended at 11:15 AM
 *   - Total volume: 5250kg
 *   - Contains: Bench Press, Overhead Press, Lateral Raise
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL // If template deleted, keep session but clear templateId
        )
    ],
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["startTime"]) // For date-based queries
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Template this session is based on (nullable for freestyle workouts).
     *
     * When starting from template:
     * - Copy exercises from template
     * - Use template name as default session name
     *
     * When freestyle:
     * - templateId = null
     * - User adds exercises manually during workout
     */
    val templateId: Long? = null,

    /**
     * Custom name for this session.
     *
     * Default: Use template name if based on template
     * Custom: User can rename (e.g., "Heavy Day", "PR Attempt")
     */
    val name: String? = null,

    /**
     * Timestamp when workout started (milliseconds since epoch).
     *
     * Used for:
     * - Sorting sessions by date
     * - Progress tracking over time
     * - Time-of-day performance analysis
     */
    val startTime: Long,

    /**
     * Timestamp when workout ended (nullable if still in progress).
     *
     * null = workout is active/in-progress
     * non-null = workout completed
     */
    val endTime: Long? = null,

    /**
     * Total volume (tonnage) for this session in kg.
     * Sum of (weight × reps) for all sets.
     *
     * Calculated when session is saved/completed.
     * Used for:
     * - Progress tracking
     * - Volume analytics
     * - Deload detection
     *
     * Example: 3 sets of 100kg x 10 = 3000kg volume
     */
    val totalVolume: Float = 0f,

    /**
     * Total number of sets completed in this session.
     * Useful for quick stats display.
     */
    val totalSets: Int = 0,

    /**
     * Average RPE across all sets (optional).
     * Calculated as mean of all RPE values.
     *
     * Used for:
     * - Intensity tracking
     * - Deload detection (if avg RPE > 9 for weeks)
     * - Recovery assessment
     */
    val averageRPE: Float? = null,

    /**
     * General notes for the entire session.
     *
     * Example:
     * - "Felt great today"
     * - "Slept poorly, struggled with weights"
     * - "New gym, different equipment"
     * - "PR day!"
     */
    val notes: String? = null,

    /**
     * User's bodyweight at time of workout (optional, in kg).
     *
     * Useful for:
     * - Wilks/Dots score calculation
     * - Relative strength tracking
     * - Bodyweight exercise progression
     */
    val bodyweight: Float? = null,

    /**
     * Location/gym where workout was performed (optional).
     * Example: "Gold's Gym", "Home", "Park"
     */
    val location: String? = null
)

/**
 * Extension functions for convenience.
 */

/**
 * Check if session is completed.
 */
fun WorkoutSessionEntity.isCompleted(): Boolean = endTime != null

/**
 * Check if session is in progress.
 */
fun WorkoutSessionEntity.isInProgress(): Boolean = endTime == null

/**
 * Get workout duration in seconds.
 * Returns null if not completed yet.
 */
fun WorkoutSessionEntity.getDurationSeconds(): Long? {
    return endTime?.let { (it - startTime) / 1000 }
}

/**
 * Get duration as display string.
 * Example: "1:15:30" (1 hour, 15 minutes, 30 seconds)
 */
fun WorkoutSessionEntity.getDurationDisplay(): String {
    val seconds = getDurationSeconds() ?: return "In progress"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

/**
 * Get display name for the session.
 * Uses custom name if set, otherwise "Workout - [date]"
 */
fun WorkoutSessionEntity.getDisplayName(): String {
    return name ?: "Workout - ${getDateDisplay()}"
}

/**
 * Get date as display string.
 * Example: "Jan 5, 2026"
 */
fun WorkoutSessionEntity.getDateDisplay(): String {
    val date = java.util.Date(startTime)
    val format = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return format.format(date)
}

/**
 * Get time as display string.
 * Example: "10:30 AM"
 */
fun WorkoutSessionEntity.getTimeDisplay(): String {
    val date = java.util.Date(startTime)
    val format = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return format.format(date)
}

/**
 * Calculate average volume per set.
 * Useful for comparing session intensity.
 */
fun WorkoutSessionEntity.getAverageVolumePerSet(): Float {
    return if (totalSets > 0) totalVolume / totalSets else 0f
}