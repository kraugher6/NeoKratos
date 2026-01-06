package com.example.neokratos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.neokratos.data.local.entity.WorkoutTemplateEntity
import com.example.neokratos.data.local.relations.TemplateWithExerciseDetails
import com.example.neokratos.data.local.relations.TemplateWithExercises
import kotlinx.coroutines.flow.Flow

/**
 * DAO for WorkoutTemplate operations.
 */
@Dao
interface WorkoutTemplateDao {

    // ===== INSERT / UPDATE / DELETE =====

    /**
     * Insert a new template.
     * Returns the ID of the inserted template.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: WorkoutTemplateEntity): Long

    /**
     * Update an existing template.
     */
    @Update
    suspend fun update(template: WorkoutTemplateEntity)

    /**
     * Delete a template.
     * Note: Cascade delete will also remove all TemplateExerciseEntity rows.
     */
    @Delete
    suspend fun delete(template: WorkoutTemplateEntity)

    /**
     * Delete template by ID.
     */
    @Query("DELETE FROM workout_templates WHERE id = :templateId")
    suspend fun deleteById(templateId: Long)

    // ===== BASIC QUERIES =====

    /**
     * Get all templates (basic info only, no exercises).
     * Ordered by name alphabetically.
     */
    @Query("SELECT * FROM workout_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<WorkoutTemplateEntity>>

    /**
     * Get template by ID (basic info only).
     */
    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    suspend fun getById(templateId: Long): WorkoutTemplateEntity?

    /**
     * Get template by ID as Flow.
     */
    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    fun getByIdFlow(templateId: Long): Flow<WorkoutTemplateEntity?>

    // ===== QUERIES WITH RELATIONS =====

    /**
     * Get template WITH its exercises (order, targets, etc).
     *
     * @Transaction ensures atomic operation (both queries succeed or fail together).
     *
     * Returns TemplateWithExercises containing:
     * - Template info (id, name)
     * - List of TemplateExerciseEntity (exerciseId, order, targets)
     *
     * Note: Does NOT include full exercise details (name, category, etc).
     * For that, use getTemplateWithExerciseDetails().
     */
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    suspend fun getTemplateWithExercises(templateId: Long): TemplateWithExercises?

    /**
     * Get template WITH exercises AND full exercise details.
     *
     * This is the most complete query - use this for displaying template in UI.
     *
     * Returns TemplateWithExerciseDetails containing:
     * - Template info
     * - List of exercises with FULL details (name, category, muscles, etc.)
     */
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    suspend fun getTemplateWithExerciseDetails(templateId: Long): TemplateWithExerciseDetails?

    /**
     * Get template with exercises as Flow (reactive).
     */
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    fun getTemplateWithExercisesFlow(templateId: Long): Flow<TemplateWithExercises?>

    /**
     * Get template with full details as Flow.
     */
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    fun getTemplateWithExerciseDetailsFlow(templateId: Long): Flow<TemplateWithExerciseDetails?>

    /**
     * Get all templates WITH their exercises.
     * Useful for template list screen showing exercise count.
     */
    @Transaction
    @Query("SELECT * FROM workout_templates ORDER BY name ASC")
    fun getAllTemplatesWithExercises(): Flow<List<TemplateWithExercises>>

    /**
     * Get all templates WITH full exercise details.
     * Use this when you need complete info for all templates.
     */
    @Transaction
    @Query("SELECT * FROM workout_templates ORDER BY name ASC")
    fun getAllTemplatesWithExerciseDetails(): Flow<List<TemplateWithExerciseDetails>>

    // ===== SEARCH =====

    /**
     * Search templates by name (case-insensitive).
     */
    @Query("""
        SELECT * FROM workout_templates 
        WHERE name LIKE '%' || :query || '%' 
        ORDER BY name ASC
    """)
    fun searchByName(query: String): Flow<List<WorkoutTemplateEntity>>

    // ===== STATS =====

    /**
     * Get total number of templates.
     */
    @Query("SELECT COUNT(*) FROM workout_templates")
    suspend fun getCount(): Int
}