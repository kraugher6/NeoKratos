package com.example.neokratos.ui.screen.activeworkout

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.neokratos.data.repository.ExerciseRepository
import com.example.neokratos.data.repository.WorkoutSessionRepository

/**
 * Factory for creating ActiveWorkoutViewModel with dependencies.
 *
 * UPDATED: Now passes Application context for service management
 */
class ActiveWorkoutViewModelFactory(
    private val application: Application,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveWorkoutViewModel::class.java)) {
            return ActiveWorkoutViewModel(
                application = application,
                workoutSessionRepository = workoutSessionRepository,
                exerciseRepository = exerciseRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}