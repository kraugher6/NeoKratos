package com.example.neokratos.data.repository

import com.example.neokratos.data.local.entity.SetEntity
import com.example.neokratos.data.local.entity.WorkoutEntity
import com.example.neokratos.data.local.dao.WorkoutDao
import android.util.Log
    class WorkoutRepository(
    private val dao: WorkoutDao
) {

    suspend fun createWorkout(): Long {
        return dao.insertWorkout(
            WorkoutEntity(
                date = System.currentTimeMillis()
            )
        )
    }

    suspend fun addSet(
        workoutId: Long,
        exercise: String,
        reps: Int,
        weight: Float
    ) {
        val newSet = SetEntity(
            workoutId = workoutId,
            exerciseName = exercise,
            reps = reps,
            weight = weight
        )
        dao.insertSet(newSet        )
        Log.d("PinoDebug", "Set insert: $newSet")
    }
}
