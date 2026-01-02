package com.example.neokratos.ui.screen.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.dao.WorkoutTemplateDao
import com.example.neokratos.data.local.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TemplateViewModel(
    private val dao: WorkoutTemplateDao
) : ViewModel() {

    val templates = dao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTemplate(name: String) {
        viewModelScope.launch {
            dao.insert(WorkoutTemplateEntity(name = name))
        }
    }
}
