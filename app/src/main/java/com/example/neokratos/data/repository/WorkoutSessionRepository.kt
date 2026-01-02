package com.example.neokratos.data.repository

import com.example.neokratos.data.local.dao.WorkoutSessionDao
import com.example.neokratos.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

class WorkoutSessionRepository(
    private val workoutSessionDao: WorkoutSessionDao
) {
    val allWorkouts: Flow<List<WorkoutSessionEntity>> =
        workoutSessionDao.getAllWorkouts()

    suspend fun startWorkout(templateId: Long? = null): Long {
        val workout = WorkoutSessionEntity(
            templateId = templateId,
            startTime = System.currentTimeMillis(),
            endTime = null
        )
        return workoutSessionDao.insertWorkout(workout)
    }
}
