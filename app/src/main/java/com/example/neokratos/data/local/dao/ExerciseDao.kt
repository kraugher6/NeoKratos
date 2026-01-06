package com.example.neokratos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.neokratos.data.local.ExerciseCategory
import com.example.neokratos.data.local.Equipment
import com.example.neokratos.data.local.MuscleGroup
import com.example.neokratos.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Exercise operations.
 *
 * All queries return Flow for reactive UI updates.
 * Suspend functions for one-shot operations (insert, update, delete).
 */
@Dao
interface ExerciseDao {

    // ===== INSERT / UPDATE / DELETE =====

    /**
     * Insert a new exercise.
     * If exercise with same ID exists, replace it (useful for wger sync).
     *
     * @return The row ID of the inserted exercise
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity): Long

    /**
     * Insert multiple exercises at once.
     * Useful for batch import from wger.
     *
     * @return List of row IDs for inserted exercises
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long>

    /**
     * Update an existing exercise.
     */
    @Update
    suspend fun update(exercise: ExerciseEntity)

    /**
     * Delete an exercise.
     * Note: This will fail if exercise is referenced by templates/sessions (FK constraint)
     */
    @Delete
    suspend fun delete(exercise: ExerciseEntity)

    // ===== BASIC QUERIES =====

    /**
     * Get all exercises ordered by name.
     * Returns Flow for reactive updates.
     */
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    /**
     * Get exercise by ID.
     */
    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getById(exerciseId: Long): ExerciseEntity?

    /**
     * Get exercise by ID as Flow (for reactive UI).
     */
    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun getByIdFlow(exerciseId: Long): Flow<ExerciseEntity?>

    // ===== SEARCH & FILTER =====

    /**
     * Search exercises by name (case-insensitive, partial match).
     * Example: "squat" will match "Front Squat", "Back Squat", etc.
     */
    @Query("""
        SELECT * FROM exercises 
        WHERE name LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchByName(query: String): Flow<List<ExerciseEntity>>

    /**
     * Filter exercises by category.
     * Example: Get all leg exercises.
     */
    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name ASC")
    fun getByCategory(category: ExerciseCategory): Flow<List<ExerciseEntity>>

    /**
     * Filter exercises by equipment.
     * Example: Get all barbell exercises.
     */
    @Query("SELECT * FROM exercises WHERE equipment = :equipment ORDER BY name ASC")
    fun getByEquipment(equipment: Equipment): Flow<List<ExerciseEntity>>

    /**
     * Filter exercises by primary muscle group.
     * Example: Get all exercises targeting quadriceps.
     */
    @Query("SELECT * FROM exercises WHERE primaryMuscleGroup = :muscle ORDER BY name ASC")
    fun getByPrimaryMuscle(muscle: MuscleGroup): Flow<List<ExerciseEntity>>

    /**
     * Get only custom exercises (created by user).
     */
    @Query("SELECT * FROM exercises WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustomExercises(): Flow<List<ExerciseEntity>>

    /**
     * Get only imported exercises (from wger).
     */
    @Query("SELECT * FROM exercises WHERE isCustom = 0 ORDER BY name ASC")
    fun getImportedExercises(): Flow<List<ExerciseEntity>>

    // ===== FAVORITES & FREQUENTLY USED =====

    /**
     * Get favorite exercises.
     */
    @Query("SELECT * FROM exercises WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<ExerciseEntity>>

    /**
     * Toggle favorite status.
     */
    @Query("UPDATE exercises SET isFavorite = :isFavorite WHERE id = :exerciseId")
    suspend fun setFavorite(exerciseId: Long, isFavorite: Boolean)

    /**
     * Get most frequently used exercises.
     * Useful for quick suggestions.
     */
    @Query("SELECT * FROM exercises ORDER BY usageCount DESC, name ASC LIMIT :limit")
    fun getMostUsed(limit: Int = 10): Flow<List<ExerciseEntity>>

    /**
     * Increment usage count for an exercise.
     * Call this when user adds exercise to a workout.
     */
    @Query("UPDATE exercises SET usageCount = usageCount + 1 WHERE id = :exerciseId")
    suspend fun incrementUsageCount(exerciseId: Long)

    // ===== WGER SYNC UTILITIES =====

    /**
     * Check if exercise from wger already exists.
     * Used to avoid duplicates during import.
     */
    @Query("SELECT * FROM exercises WHERE wgerId = :wgerId LIMIT 1")
    suspend fun getByWgerId(wgerId: Int): ExerciseEntity?

    /**
     * Get all wger IDs currently in database.
     * Useful for bulk sync operations.
     */
    @Query("SELECT wgerId FROM exercises WHERE wgerId IS NOT NULL")
    suspend fun getAllWgerIds(): List<Int>

    // ===== COUNT & STATS =====

    /**
     * Get total number of exercises.
     */
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int

    /**
     * Get count of custom exercises.
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE isCustom = 1")
    suspend fun getCustomCount(): Int

    /**
     * Get count by category.
     * Useful for stats screen: "You have 15 leg exercises"
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE category = :category")
    suspend fun getCountByCategory(category: ExerciseCategory): Int
}