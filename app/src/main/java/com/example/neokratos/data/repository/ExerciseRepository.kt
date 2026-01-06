package com.example.neokratos.data.repository

import com.example.neokratos.data.local.ExerciseCategory
import com.example.neokratos.data.local.Equipment
import com.example.neokratos.data.local.MuscleGroup
import com.example.neokratos.data.local.dao.ExerciseDao
import com.example.neokratos.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Exercise operations.
 *
 * This is the single source of truth for exercise data.
 * ViewModels should always go through this repository, never directly to DAO.
 *
 * Why use Repository pattern?
 * - Centralize data access logic
 * - Easy to add caching, network calls (wger API), etc.
 * - Easy to mock for testing
 * - Business logic separated from DAO (which is just SQL)
 */
class ExerciseRepository(
    private val exerciseDao: ExerciseDao
) {

    // ===== EXPOSED FLOWS (for UI) =====

    /**
     * All exercises, sorted alphabetically.
     * UI observes this Flow and updates automatically when data changes.
     */
    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAll()

    /**
     * Favorite exercises only.
     */
    val favoriteExercises: Flow<List<ExerciseEntity>> = exerciseDao.getFavorites()

    /**
     * Most frequently used exercises (top 10).
     */
    val mostUsedExercises: Flow<List<ExerciseEntity>> = exerciseDao.getMostUsed(10)

    /**
     * Custom exercises created by user.
     */
    val customExercises: Flow<List<ExerciseEntity>> = exerciseDao.getCustomExercises()

    // ===== INSERT / UPDATE / DELETE =====

    /**
     * Insert a new exercise.
     * Returns the ID of the inserted exercise.
     */
    suspend fun insert(exercise: ExerciseEntity): Long {
        return exerciseDao.insert(exercise)
    }

    /**
     * Bulk insert exercises.
     * Useful for importing from wger.
     * Returns list of inserted IDs.
     */
    suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long> {
        return exerciseDao.insertAll(exercises)
    }

    /**
     * Update an existing exercise.
     */
    suspend fun update(exercise: ExerciseEntity) {
        exerciseDao.update(exercise)
    }

    /**
     * Delete an exercise.
     * Note: Will fail if exercise is used in templates/sessions (FK constraint).
     */
    suspend fun delete(exercise: ExerciseEntity) {
        exerciseDao.delete(exercise)
    }

    /**
     * Delete exercise by ID.
     */
    suspend fun deleteById(exerciseId: Long) {
        val exercise = exerciseDao.getById(exerciseId)
        exercise?.let { exerciseDao.delete(it) }
    }

    // ===== QUERIES =====

    /**
     * Get exercise by ID (one-shot).
     */
    suspend fun getById(exerciseId: Long): ExerciseEntity? {
        return exerciseDao.getById(exerciseId)
    }

    /**
     * Get exercise by ID as Flow (reactive).
     */
    fun getByIdFlow(exerciseId: Long): Flow<ExerciseEntity?> {
        return exerciseDao.getByIdFlow(exerciseId)
    }

    /**
     * Search exercises by name.
     * Case-insensitive, partial match.
     */
    fun searchByName(query: String): Flow<List<ExerciseEntity>> {
        return exerciseDao.searchByName(query)
    }

    /**
     * Filter exercises by category.
     */
    fun getByCategory(category: ExerciseCategory): Flow<List<ExerciseEntity>> {
        return exerciseDao.getByCategory(category)
    }

    /**
     * Filter exercises by equipment.
     */
    fun getByEquipment(equipment: Equipment): Flow<List<ExerciseEntity>> {
        return exerciseDao.getByEquipment(equipment)
    }

    /**
     * Filter exercises by primary muscle group.
     */
    fun getByPrimaryMuscle(muscle: MuscleGroup): Flow<List<ExerciseEntity>> {
        return exerciseDao.getByPrimaryMuscle(muscle)
    }

    // ===== FAVORITES =====

    /**
     * Toggle favorite status for an exercise.
     */
    suspend fun toggleFavorite(exerciseId: Long) {
        val exercise = exerciseDao.getById(exerciseId)
        exercise?.let {
            exerciseDao.setFavorite(exerciseId, !it.isFavorite)
        }
    }

    /**
     * Set favorite status explicitly.
     */
    suspend fun setFavorite(exerciseId: Long, isFavorite: Boolean) {
        exerciseDao.setFavorite(exerciseId, isFavorite)
    }

    // ===== USAGE TRACKING =====

    /**
     * Increment usage count for an exercise.
     * Call this whenever user adds exercise to a workout.
     */
    suspend fun incrementUsageCount(exerciseId: Long) {
        exerciseDao.incrementUsageCount(exerciseId)
    }

    // ===== STATS =====

    /**
     * Get total number of exercises in library.
     */
    suspend fun getTotalCount(): Int {
        return exerciseDao.getCount()
    }

    /**
     * Get number of custom exercises.
     */
    suspend fun getCustomCount(): Int {
        return exerciseDao.getCustomCount()
    }

    /**
     * Get count by category.
     */
    suspend fun getCountByCategory(category: ExerciseCategory): Int {
        return exerciseDao.getCountByCategory(category)
    }

    // ===== WGER SYNC (Future implementation) =====

    /**
     * Check if exercise from wger already exists.
     * Used during import to avoid duplicates.
     */
    suspend fun getByWgerId(wgerId: Int): ExerciseEntity? {
        return exerciseDao.getByWgerId(wgerId)
    }

    /**
     * Get all wger IDs currently stored.
     * Useful for checking what needs to be synced.
     */
    suspend fun getAllWgerIds(): List<Int> {
        return exerciseDao.getAllWgerIds()
    }

    // ===== SEED DATA (for initial setup) =====

    /**
     * Check if database is empty and needs seed data.
     */
    suspend fun needsSeedData(): Boolean {
        return exerciseDao.getCount() == 0
    }

    /**
     * Insert basic exercises for initial setup.
     * Call this on first app launch.
     */
    suspend fun insertSeedExercises() {
        val seedExercises = listOf(
            // Legs
            ExerciseEntity(
                name = "Back Squat",
                description = "Barbell back squat",
                category = ExerciseCategory.LEGS,
                primaryMuscleGroup = MuscleGroup.QUADRICEPS,
                secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
                equipment = Equipment.BARBELL,
                isCustom = false
            ),
            ExerciseEntity(
                name = "Front Squat",
                description = "Barbell front squat",
                category = ExerciseCategory.LEGS,
                primaryMuscleGroup = MuscleGroup.QUADRICEPS,
                secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
                equipment = Equipment.BARBELL,
                isCustom = false
            ),
            ExerciseEntity(
                name = "Romanian Deadlift",
                description = "Romanian deadlift focusing on hamstrings",
                category = ExerciseCategory.LEGS,
                primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
                secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
                equipment = Equipment.BARBELL,
                isCustom = false
            ),

            // Chest
            ExerciseEntity(
                name = "Bench Press",
                description = "Barbell bench press",
                category = ExerciseCategory.CHEST,
                primaryMuscleGroup = MuscleGroup.PECTORALS_MIDDLE,
                secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.DELTS_ANTERIOR),
                equipment = Equipment.BARBELL,
                isCustom = false
            ),
            ExerciseEntity(
                name = "Incline Dumbbell Press",
                description = "Incline dumbbell press for upper chest",
                category = ExerciseCategory.CHEST,
                primaryMuscleGroup = MuscleGroup.PECTORALS_UPPER,
                secondaryMuscleGroups = listOf(MuscleGroup.DELTS_ANTERIOR, MuscleGroup.TRICEPS),
                equipment = Equipment.DUMBBELL,
                isCustom = false
            ),

            // Back
            ExerciseEntity(
                name = "Pull-ups",
                description = "Bodyweight pull-ups",
                category = ExerciseCategory.BACK,
                primaryMuscleGroup = MuscleGroup.LATS,
                secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.TRAPS),
                equipment = Equipment.BODYWEIGHT,
                isCustom = false
            ),
            ExerciseEntity(
                name = "Barbell Row",
                description = "Bent-over barbell row",
                category = ExerciseCategory.BACK,
                primaryMuscleGroup = MuscleGroup.LATS,
                secondaryMuscleGroups = listOf(MuscleGroup.TRAPS, MuscleGroup.RHOMBOIDS, MuscleGroup.BICEPS),
                equipment = Equipment.BARBELL,
                isCustom = false
            ),

            // Shoulders
            ExerciseEntity(
                name = "Overhead Press",
                description = "Standing barbell overhead press",
                category = ExerciseCategory.SHOULDERS,
                primaryMuscleGroup = MuscleGroup.DELTS_ANTERIOR,
                secondaryMuscleGroups = listOf(MuscleGroup.DELTS_LATERAL, MuscleGroup.TRICEPS),
                equipment = Equipment.BARBELL,
                isCustom = false
            ),
            ExerciseEntity(
                name = "Lateral Raise",
                description = "Dumbbell lateral raise",
                category = ExerciseCategory.SHOULDERS,
                primaryMuscleGroup = MuscleGroup.DELTS_LATERAL,
                secondaryMuscleGroups = emptyList(),
                equipment = Equipment.DUMBBELL,
                isCustom = false
            ),

            // Arms
            ExerciseEntity(
                name = "Barbell Curl",
                description = "Standing barbell curl",
                category = ExerciseCategory.ARMS,
                primaryMuscleGroup = MuscleGroup.BICEPS,
                secondaryMuscleGroups = listOf(MuscleGroup.FOREARMS),
                equipment = Equipment.BARBELL,
                isCustom = false
            ),
            ExerciseEntity(
                name = "Tricep Dips",
                description = "Bodyweight tricep dips",
                category = ExerciseCategory.ARMS,
                primaryMuscleGroup = MuscleGroup.TRICEPS,
                secondaryMuscleGroups = listOf(MuscleGroup.PECTORALS_LOWER, MuscleGroup.DELTS_ANTERIOR),
                equipment = Equipment.BODYWEIGHT,
                isCustom = false
            ),

            // Core
            ExerciseEntity(
                name = "Plank",
                description = "Front plank hold",
                category = ExerciseCategory.CORE,
                primaryMuscleGroup = MuscleGroup.ABS,
                secondaryMuscleGroups = listOf(MuscleGroup.OBLIQUES),
                equipment = Equipment.BODYWEIGHT,
                isCustom = false
            )
        )

        insertAll(seedExercises)
    }
}