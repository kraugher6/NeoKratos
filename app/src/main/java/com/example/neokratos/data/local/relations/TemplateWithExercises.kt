package com.example.neokratos.data.local.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.neokratos.data.local.entity.TemplateExerciseEntity
import com.example.neokratos.data.local.entity.WorkoutTemplateEntity

/**
 * Represents a complete workout template with all its exercises.
 *
 * Used by UI to display:
 * - Template name
 * - List of exercises with order, target sets/reps, etc.
 *
 * Room automatically populates this by joining tables:
 * WorkoutTemplateEntity (1) ←→ (N) TemplateExerciseEntity
 *
 * Example usage:
 * ```
 * val template = templateDao.getTemplateWithExercises(1)
 * println(template.template.name) // "Push Day"
 * template.exercises.forEach { ex ->
 *     println("${ex.order}: Exercise ID ${ex.exerciseId} - ${ex.getSetsRepsDisplay()}")
 * }
 * ```
 */
data class TemplateWithExercises(
    /**
     * The template itself (id, name, createdAt).
     * @Embedded = flatten all fields of WorkoutTemplateEntity into this class.
     */
    @Embedded
    val template: WorkoutTemplateEntity,

    /**
     * List of exercises in this template, ordered by position.
     *
     * @Relation tells Room:
     * - parentColumn = "id" (from WorkoutTemplateEntity)
     * - entityColumn = "templateId" (from TemplateExerciseEntity)
     *
     * Room executes:
     * SELECT * FROM template_exercises WHERE templateId = template.id
     */
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val exercises: List<TemplateExerciseEntity>
)

/**
 * Represents a template exercise WITH full exercise details from library.
 *
 * Contains:
 * - Template exercise info (order, target sets/reps, notes)
 * - Full exercise details (name, category, muscle groups, etc.)
 *
 * This is what you actually display in UI:
 * "1. Bench Press - 3x8-12 - Chest/Triceps"
 */
data class TemplateExerciseWithDetails(
    /**
     * Template exercise (order, targets, etc.)
     */
    @Embedded
    val templateExercise: TemplateExerciseEntity,

    /**
     * Full exercise details from library.
     *
     * @Relation joins:
     * templateExercise.exerciseId → ExerciseEntity.id
     */
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: com.example.neokratos.data.local.entity.ExerciseEntity
)

/**
 * Complete template with exercises AND their full details.
 *
 * This is the MOST USEFUL data class for UI.
 * Contains everything you need to display a template screen.
 *
 * Structure:
 * - Template metadata (name, date)
 * - List of exercises with:
 *   - Position/order
 *   - Target sets/reps
 *   - Exercise name, category, muscles
 */
data class TemplateWithExerciseDetails(
    @Embedded
    val template: WorkoutTemplateEntity,

    @Relation(
        entity = TemplateExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val exercisesWithDetails: List<TemplateExerciseWithDetails>
)

/**
 * Extension functions for convenience.
 */

/**
 * Get total number of exercises in template.
 */
fun TemplateWithExercises.getExerciseCount(): Int = exercises.size

/**
 * Get total number of sets in template (sum of all target sets).
 * Example: 3 exercises with 3 sets each = 9 total sets
 */
fun TemplateWithExercises.getTotalSets(): Int = exercises.sumOf { it.targetSets }

/**
 * Get estimated workout duration in minutes.
 * Rough calculation:
 * - Each set takes ~2 minutes (1 min work + 1 min rest)
 * - Add 5 min warmup
 */
fun TemplateWithExercises.getEstimatedDuration(): Int {
    val totalSets = getTotalSets()
    return (totalSets * 2) + 5 // minutes
}

/**
 * Check if template is empty.
 */
fun TemplateWithExercises.isEmpty(): Boolean = exercises.isEmpty()

/**
 * Get exercises sorted by order (just in case).
 */
fun TemplateWithExercises.getSortedExercises(): List<TemplateExerciseEntity> {
    return exercises.sortedBy { it.order }
}

/**
 * Get display string for template summary.
 * Example: "Push Day - 5 exercises, 15 sets, ~35 min"
 */
fun TemplateWithExercises.getSummaryDisplay(): String {
    val exerciseCount = getExerciseCount()
    val totalSets = getTotalSets()
    val duration = getEstimatedDuration()
    return "${template.name} - $exerciseCount exercises, $totalSets sets, ~$duration min"
}