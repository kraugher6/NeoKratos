package com.example.neokratos.ui.screen.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.neokratos.data.local.dao.TemplateExerciseDao
import com.example.neokratos.data.local.dao.WorkoutTemplateDao

class TemplateEditViewModelFactory(
    private val templateDao: WorkoutTemplateDao,
    private val templateExerciseDao: TemplateExerciseDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TemplateEditViewModel::class.java)) {
            return TemplateEditViewModel(templateDao, templateExerciseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}