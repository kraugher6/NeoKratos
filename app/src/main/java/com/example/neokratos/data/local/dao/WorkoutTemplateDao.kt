package com.example.neokratos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.neokratos.data.local.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Query("SELECT * FROM workout_templates ORDER BY name")
    fun getAllTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Insert
    suspend fun insert(template: WorkoutTemplateEntity)
}
