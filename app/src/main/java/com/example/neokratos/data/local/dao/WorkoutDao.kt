package com.example.neokratos.data.local.dao

import com.example.neokratos.data.local.entity.WorkoutEntity
import androidx.room.Dao
import androidx.room.Insert
import com.example.neokratos.data.local.entity.SetEntity

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertSet(set: SetEntity)
}
