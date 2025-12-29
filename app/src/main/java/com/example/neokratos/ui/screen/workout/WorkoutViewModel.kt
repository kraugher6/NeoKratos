package com.example.neokratos.ui.screen.workout

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.repository.WorkoutRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    var currentWorkoutId by mutableStateOf<Long?>(null)
        private set

    fun startWorkout() {
        viewModelScope.launch {
            currentWorkoutId = repository.createWorkout()
        }
    }

    fun addSet(
        exercise: String,
        reps: Int,
        weight: Float
    ) {
        val workoutId = currentWorkoutId ?: return
        viewModelScope.launch {
            repository.addSet(
                workoutId = workoutId,
                exercise = exercise,
                reps = reps,
                weight = weight
            )
        }
    }
}
