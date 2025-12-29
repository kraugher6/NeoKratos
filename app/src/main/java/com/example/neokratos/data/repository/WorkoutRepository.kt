package com.example.neokratos.data.repository

import com.example.neokratos.data.local.dao.WorkoutDao
import com.example.neokratos.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao
) {
    val allWorkouts: Flow<List<WorkoutEntity>> =
        workoutDao.getAllWorkouts()

    suspend fun startWorkout(): Long {
        val workout = WorkoutEntity(startTime = System.currentTimeMillis())
        return workoutDao.insertWorkout(workout)
    }
}
