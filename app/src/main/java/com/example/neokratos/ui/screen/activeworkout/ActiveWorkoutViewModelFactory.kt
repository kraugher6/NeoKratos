package com.example.neokratos.ui.screen.activeworkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.neokratos.data.repository.ExerciseRepository
import com.example.neokratos.data.repository.WorkoutSessionRepository

/**
 * Factory for creating ActiveWorkoutViewModel with dependencies.
 */
class ActiveWorkoutViewModelFactory(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveWorkoutViewModel::class.java)) {
            return ActiveWorkoutViewModel(
                workoutSessionRepository = workoutSessionRepository,
                exerciseRepository = exerciseRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}