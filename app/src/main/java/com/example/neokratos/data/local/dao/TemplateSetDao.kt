package com.example.neokratos.data.local.dao

import androidx.room.*
import com.example.neokratos.data.local.entity.TemplateSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateSetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(templateSet: TemplateSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templateSets: List<TemplateSetEntity>): List<Long>

    @Update
    suspend fun update(templateSet: TemplateSetEntity)

    @Delete
    suspend fun delete(templateSet: TemplateSetEntity)

    @Query("DELETE FROM template_sets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM template_sets WHERE templateExerciseId = :templateExerciseId")
    suspend fun deleteAllForTemplateExercise(templateExerciseId: Long)

    @Query("""
        SELECT * FROM template_sets 
        WHERE templateExerciseId = :templateExerciseId 
        ORDER BY setNumber ASC
    """)
    fun getSetsForExercise(templateExerciseId: Long): Flow<List<TemplateSetEntity>>

    @Query("""
        SELECT * FROM template_sets 
        WHERE templateExerciseId = :templateExerciseId 
        ORDER BY setNumber ASC
    """)
    suspend fun getSetsForExerciseOneShot(templateExerciseId: Long): List<TemplateSetEntity>

    @Query("SELECT * FROM template_sets WHERE id = :id")
    suspend fun getById(id: Long): TemplateSetEntity?

    @Query("SELECT COUNT(*) FROM template_sets WHERE templateExerciseId = :templateExerciseId")
    suspend fun getSetCount(templateExerciseId: Long): Int

    @Query("UPDATE template_sets SET setNumber = :newNumber WHERE id = :id")
    suspend fun updateSetNumber(id: Long, newNumber: Int)

    @Transaction
    suspend fun reorderSets(updates: List<Pair<Long, Int>>) {
        updates.forEach { (id, number) ->
            updateSetNumber(id, number)
        }
    }
}