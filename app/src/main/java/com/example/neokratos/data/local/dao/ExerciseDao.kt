package com.example.neokratos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.neokratos.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert
    suspend fun insert(exercise: ExerciseEntity)

    @Query("""
        SELECT * FROM exercises 
        WHERE workoutTemplateId = :templateId
        ORDER BY `order`
    """)
    fun getExercisesForTemplate(templateId: Long): Flow<List<ExerciseEntity>>
}
