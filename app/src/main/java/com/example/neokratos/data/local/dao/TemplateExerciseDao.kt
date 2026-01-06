package com.example.neokratos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.neokratos.data.local.entity.TemplateExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for TemplateExercise operations.
 *
 * Manages the relationship between templates and exercises.
 */
@Dao
interface TemplateExerciseDao {

    // ===== INSERT / UPDATE / DELETE =====

    /**
     * Insert an exercise into a template.
     * Returns the ID of the inserted row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(templateExercise: TemplateExerciseEntity): Long

    /**
     * Insert multiple exercises at once.
     * Useful when creating a template with multiple exercises.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templateExercises: List<TemplateExerciseEntity>): List<Long>

    /**
     * Update an existing template exercise.
     * Use this to modify target sets/reps, order, notes, etc.
     */
    @Update
    suspend fun update(templateExercise: TemplateExerciseEntity)

    /**
     * Delete a template exercise.
     */
    @Delete
    suspend fun delete(templateExercise: TemplateExerciseEntity)

    /**
     * Delete template exercise by ID.
     */
    @Query("DELETE FROM template_exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Delete all exercises from a template.
     * Called when clearing/resetting a template.
     */
    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteAllForTemplate(templateId: Long)

    // ===== QUERIES =====

    /**
     * Get all exercises for a specific template, ordered by position.
     * Returns Flow for reactive UI updates.
     *
     * NOTE: This returns only TemplateExerciseEntity (IDs).
     * To get full exercise details, use getTemplateWithExercises() instead.
     */
    @Query("""
        SELECT * FROM template_exercises 
        WHERE templateId = :templateId 
        ORDER BY `order` ASC
    """)
    fun getForTemplate(templateId: Long): Flow<List<TemplateExerciseEntity>>

    /**
     * Get single template exercise by ID.
     */
    @Query("SELECT * FROM template_exercises WHERE id = :id")
    suspend fun getById(id: Long): TemplateExerciseEntity?

    /**
     * Get all templates that use a specific exercise.
     * Useful for showing "Used in N templates" in exercise detail.
     */
    @Query("""
        SELECT DISTINCT templateId 
        FROM template_exercises 
        WHERE exerciseId = :exerciseId
    """)
    suspend fun getTemplateIdsUsingExercise(exerciseId: Long): List<Long>

    /**
     * Check if an exercise is used in any template.
     * Useful before allowing exercise deletion.
     */
    @Query("""
        SELECT COUNT(*) > 0 
        FROM template_exercises 
        WHERE exerciseId = :exerciseId
    """)
    suspend fun isExerciseUsedInTemplates(exerciseId: Long): Boolean

    /**
     * Count exercises in a template.
     */
    @Query("SELECT COUNT(*) FROM template_exercises WHERE templateId = :templateId")
    suspend fun getExerciseCount(templateId: Long): Int

    // ===== REORDERING =====

    /**
     * Update order for a specific exercise.
     * Used when drag-and-drop reordering.
     */
    @Query("UPDATE template_exercises SET `order` = :newOrder WHERE id = :id")
    suspend fun updateOrder(id: Long, newOrder: Int)

    /**
     * Batch update orders for multiple exercises.
     * More efficient than updating one by one.
     */
    @Transaction
    suspend fun reorderExercises(updates: List<Pair<Long, Int>>) {
        updates.forEach { (id, order) ->
            updateOrder(id, order)
        }
    }

    // ===== TEMPLATE CLONING =====

    /**
     * Get all exercises from a template (for cloning).
     * Returns list without IDs (for inserting as new rows).
     */
    @Query("""
        SELECT * FROM template_exercises 
        WHERE templateId = :sourceTemplateId 
        ORDER BY `order` ASC
    """)
    suspend fun getForCloning(sourceTemplateId: Long): List<TemplateExerciseEntity>
}