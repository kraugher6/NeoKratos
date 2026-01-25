package com.example.neokratos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Junction entity linking WorkoutTemplate to Exercise.
 *
 * Represents an exercise within a template (workout plan).
 * Contains the "blueprint" for what you plan to do.
 *
 * Example:
 * Template "Push Day" contains:
 * - Bench Press: 3 sets of 8-12 reps
 * - Overhead Press: 3 sets of 8-12 reps
 * - Lateral Raise: 3 sets of 12-15 reps
 *
 * Foreign Keys:
 * - templateId → WorkoutTemplateEntity (cascade delete: if template deleted, remove all its exercises)
 * - exerciseId → ExerciseEntity (restrict: can't delete exercise used in templates)
 */
@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE // Delete exercises when template is deleted
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT // Prevent deleting exercise used in templates
        )
    ],
    indices = [
        Index(value = ["templateId"]),  // Fast lookup of exercises for a template
        Index(value = ["exerciseId"])   // Fast lookup of templates using an exercise
    ]
)
data class TemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Which template this exercise belongs to.
     */
    val templateId: Long,

    /**
     * Which exercise from the library.
     * References ExerciseEntity.id
     */
    val exerciseId: Long,

    /**
     * Order/position in the template.
     * 0 = first exercise, 1 = second, etc.
     *
     * Used for:
     * - Displaying exercises in correct order
     * - Drag & drop reordering
     */
    val order: Int,

    /**
     * Target number of sets.
     * Example: 3 for "3 sets"
     */
    val targetSets: Int = 3,

    /**
     * Minimum target reps.
     * For rep ranges like 8-12, this is 8.
     * For fixed reps like 5, this equals targetRepsMax.
     */
    val targetRepsMin: Int = 8,

    /**
     * Maximum target reps.
     * For rep ranges like 8-12, this is 12.
     * For fixed reps like 5, this equals targetRepsMin.
     */
    val targetRepsMax: Int = 12,

    /**
     * Optional notes for this exercise in the template.
     * Example: "Focus on slow eccentric", "Pause at bottom", "Use tempo 3-0-1"
     */
    val notes: String? = null,

    /**
     * Rest time in seconds between sets (optional).
     * If null, use default rest time from settings.
     * Example: 90 = 90 seconds = 1:30 rest
     */
    val restSeconds: Int = 90
)

/**
 * Extension functions for convenience.
 */

/**
 * Get rep range as display string.
 * Examples:
 * - targetRepsMin = 8, targetRepsMax = 12 → "8-12"
 * - targetRepsMin = 5, targetRepsMax = 5 → "5"
 */
fun TemplateExerciseEntity.getRepRangeDisplay(): String {
    return if (targetRepsMin == targetRepsMax) {
        "$targetRepsMin"
    } else {
        "$targetRepsMin-$targetRepsMax"
    }
}

/**
 * Get sets x reps display.
 * Examples:
 * - 3 sets of 8-12 → "3x8-12"
 * - 5 sets of 5 → "5x5"
 */
fun TemplateExerciseEntity.getSetsRepsDisplay(): String {
    return "${targetSets}x${getRepRangeDisplay()}"
}

/**
 * Get rest time display.
 * Examples:
 * - 90 seconds → "1:30"
 * - 120 seconds → "2:00"
 * - null → "Default"
 */
fun TemplateExerciseEntity.getRestTimeDisplay(): String {
    return restSeconds?.let {
        val minutes = it / 60
        val seconds = it % 60
        String.format("%d:%02d", minutes, seconds)
    } ?: "Default"
}