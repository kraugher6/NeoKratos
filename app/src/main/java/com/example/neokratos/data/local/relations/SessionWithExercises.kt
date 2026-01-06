package com.example.neokratos.data.local.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.neokratos.data.local.entity.SessionExerciseEntity
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.entity.WorkoutSessionEntity
import com.example.neokratos.data.local.entity.estimateOneRepMax
import com.example.neokratos.data.local.entity.getDurationDisplay
import com.example.neokratos.data.local.entity.getVolume
import com.example.neokratos.data.local.entity.isCompleted

/**
 * Represents a workout session with all its exercises.
 *
 * Used for displaying session in history or during active workout.
 * Contains session metadata + list of exercises (without set details).
 */
data class SessionWithExercises(
    @Embedded
    val session: WorkoutSessionEntity,

    /**
     * List of exercises in this session, ordered by position.
     */
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val exercises: List<SessionExerciseEntity>
)

/**
 * Represents a session exercise WITH all its sets.
 *
 * This is what you display during workout:
 * - Exercise info (name, order, notes)
 * - All sets with weight/reps/RPE
 */
data class SessionExerciseWithSets(
    @Embedded
    val sessionExercise: SessionExerciseEntity,

    /**
     * All sets for this exercise, ordered by set number.
     */
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionExerciseId"
    )
    val sets: List<SetLogEntity>
)

/**
 * Represents a session exercise WITH sets AND exercise details from library.
 *
 * Complete info for displaying during workout:
 * - Exercise metadata (name, category, muscles)
 * - Session exercise info (order, notes, timestamps)
 * - All sets (weight, reps, RPE)
 */
data class SessionExerciseWithDetails(
    @Embedded
    val sessionExercise: SessionExerciseEntity,

    /**
     * Exercise details from library (name, category, etc.)
     */
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: com.example.neokratos.data.local.entity.ExerciseEntity,

    /**
     * All sets for this exercise.
     */
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionExerciseId"
    )
    val sets: List<SetLogEntity>
)

/**
 * Complete workout session with EVERYTHING.
 *
 * This is the MOST USEFUL data class for UI.
 * Contains all data needed to display:
 * - Active workout screen
 * - Workout history detail
 * - Workout summary
 *
 * Structure:
 * - Session metadata (name, date, duration, volume)
 * - List of exercises with:
 *   - Exercise details (name, category, muscles)
 *   - All sets (weight, reps, RPE, rest)
 */
data class SessionComplete(
    @Embedded
    val session: WorkoutSessionEntity,

    @Relation(
        entity = SessionExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val exercisesWithDetails: List<SessionExerciseWithDetails>
)

/**
 * Extension functions for convenience.
 */

// ===== SessionWithExercises extensions =====

fun SessionWithExercises.getExerciseCount(): Int = exercises.size

fun SessionWithExercises.getCompletedExerciseCount(): Int =
    exercises.count { it.isCompleted() }

fun SessionWithExercises.isEmpty(): Boolean = exercises.isEmpty()

fun SessionWithExercises.isAllCompleted(): Boolean =
    exercises.isNotEmpty() && exercises.all { it.isCompleted() }

fun SessionWithExercises.getProgressPercentage(): Int {
    if (exercises.isEmpty()) return 0
    return (getCompletedExerciseCount() * 100) / exercises.size
}

// ===== SessionExerciseWithSets extensions =====

fun SessionExerciseWithSets.getSetCount(): Int = sets.size

fun SessionExerciseWithSets.getCompletedSetCount(): Int =
    sets.count { it.completed }

fun SessionExerciseWithSets.getTotalVolume(): Float =
    sets.filter { it.completed }.sumOf { it.getVolume().toDouble() }.toFloat()

fun SessionExerciseWithSets.getAverageRPE(): Float? {
    val rpeSets = sets.filter { it.completed && it.rpe != null }
    if (rpeSets.isEmpty()) return null
    return rpeSets.mapNotNull { it.rpe }.average().toFloat()
}

fun SessionExerciseWithSets.getMaxWeight(): Float? =
    sets.filter { it.completed }.maxOfOrNull { it.weight }

fun SessionExerciseWithSets.getBestEstimated1RM(): Float? =
    sets.filter { it.completed }.maxOfOrNull { it.estimateOneRepMax() }

fun SessionExerciseWithSets.isEmpty(): Boolean = sets.isEmpty()

/**
 * Get next set number to log.
 * Example: if you have 3 sets, next is 4.
 */
fun SessionExerciseWithSets.getNextSetNumber(): Int = sets.size + 1

/**
 * Get summary string for display.
 * Example: "3 sets, 900kg volume, RPE 8.5"
 */
fun SessionExerciseWithSets.getSummary(): String {
    val setCount = getCompletedSetCount()
    val volume = getTotalVolume()
    val avgRPE = getAverageRPE()

    return buildString {
        append("$setCount sets")
        if (volume > 0) append(", ${volume.toInt()}kg")
        avgRPE?.let { append(", RPE %.1f".format(it)) }
    }
}

// ===== SessionExerciseWithDetails extensions =====

fun SessionExerciseWithDetails.getSetCount(): Int = sets.size

fun SessionExerciseWithDetails.getCompletedSetCount(): Int =
    sets.count { it.completed }

fun SessionExerciseWithDetails.getTotalVolume(): Float =
    sets.filter { it.completed }.sumOf { it.getVolume().toDouble() }.toFloat()

fun SessionExerciseWithDetails.getAverageRPE(): Float? {
    val rpeSets = sets.filter { it.completed && it.rpe != null }
    if (rpeSets.isEmpty()) return null
    return rpeSets.mapNotNull { it.rpe }.average().toFloat()
}

fun SessionExerciseWithDetails.getNextSetNumber(): Int = sets.size + 1

fun SessionExerciseWithDetails.isEmpty(): Boolean = sets.isEmpty()

// ===== SessionComplete extensions =====

fun SessionComplete.getTotalExercises(): Int = exercisesWithDetails.size

fun SessionComplete.getTotalSets(): Int =
    exercisesWithDetails.sumOf { it.sets.count { set -> set.completed } }

fun SessionComplete.getTotalVolume(): Float =
    exercisesWithDetails.sumOf {
        it.sets.filter { set -> set.completed }.sumOf { set -> set.getVolume().toDouble() }
    }.toFloat()

fun SessionComplete.getAverageRPE(): Float? {
    val allRPEs = exercisesWithDetails
        .flatMap { it.sets }
        .filter { it.completed && it.rpe != null }
        .mapNotNull { it.rpe }

    if (allRPEs.isEmpty()) return null
    return allRPEs.average().toFloat()
}

fun SessionComplete.isEmpty(): Boolean = exercisesWithDetails.isEmpty()

/**
 * Get workout summary for display.
 * Example: "5 exercises, 15 sets, 4500kg volume, 75 min"
 */
fun SessionComplete.getWorkoutSummary(): String {
    val exercises = getTotalExercises()
    val sets = getTotalSets()
    val volume = getTotalVolume()
    val duration = session.getDurationDisplay()

    return buildString {
        append("$exercises exercises, $sets sets")
        if (volume > 0) append(", ${volume.toInt()}kg")
        if (session.isCompleted()) append(", $duration")
    }
}

/**
 * Calculate workout intensity score (0-100).
 * Based on average RPE and volume.
 *
 * Formula: (avgRPE / 10) * 0.7 + (volume / targetVolume) * 0.3
 * Simple heuristic for now, can be improved.
 */
fun SessionComplete.getIntensityScore(): Float? {
    val avgRPE = getAverageRPE() ?: return null
    val rpeScore = (avgRPE / 10f) * 100f
    return rpeScore // Simplified for now
}

/**
 * Get exercises sorted by order.
 */
fun SessionComplete.getSortedExercises(): List<SessionExerciseWithDetails> {
    return exercisesWithDetails.sortedBy { it.sessionExercise.order }
}

/**
 * Check if workout is completed (all exercises have endTime).
 */
fun SessionComplete.isWorkoutCompleted(): Boolean {
    if (exercisesWithDetails.isEmpty()) return false
    return exercisesWithDetails.all { it.sessionExercise.isCompleted() }
}