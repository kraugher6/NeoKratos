package com.example.neokratos.ui.screen.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.neokratos.data.repository.ExerciseRepository

class ExerciseLibraryViewModelFactory(
    private val exerciseRepository: ExerciseRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseLibraryViewModel::class.java)) {
            return ExerciseLibraryViewModel(exerciseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}