package com.example.neokratos.ui.screen.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    var currentWorkoutId: Long? = null
        private set


    fun startWorkout() {
        viewModelScope.launch(Dispatchers.IO) {
            currentWorkoutId = repository.startWorkout()
        }
    }
}
