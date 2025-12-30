package com.example.neokratos.ui.screen.workouttemplate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.entity.WorkoutTemplateEntity
import com.example.neokratos.data.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutTemplateViewModel(
    private val repository: WorkoutTemplateRepository
) : ViewModel() {

    val templates: StateFlow<List<WorkoutTemplateEntity>> =
        repository.allTemplates.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun addTemplate(name: String) {
        viewModelScope.launch {
            repository.insert(WorkoutTemplateEntity(name = name))
        }
    }
}
