package com.example.neokratos.ui.screen.workouttemplate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.neokratos.data.repository.WorkoutTemplateRepository

class WorkoutTemplateViewModelFactory(
    private val repository: WorkoutTemplateRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutTemplateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutTemplateViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

