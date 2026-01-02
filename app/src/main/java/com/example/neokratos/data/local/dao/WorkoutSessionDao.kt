package com.example.neokratos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.neokratos.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Insert
    suspend fun insertWorkout(workout: WorkoutSessionEntity): Long

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>>
}
