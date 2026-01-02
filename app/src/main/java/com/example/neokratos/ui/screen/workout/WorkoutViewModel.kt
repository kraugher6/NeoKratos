package com.example.neokratos.ui.screen.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val repository: WorkoutSessionRepository
) : ViewModel() {

    var currentWorkoutId: Long? = null
        private set


    fun startWorkout() {
        viewModelScope.launch(Dispatchers.IO) {
            currentWorkoutId = repository.startWorkout()
        }
    }
}
