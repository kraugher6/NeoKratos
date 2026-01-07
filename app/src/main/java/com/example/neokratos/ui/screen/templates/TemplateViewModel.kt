package com.example.neokratos.ui.screen.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.dao.WorkoutTemplateDao
import com.example.neokratos.data.local.entity.WorkoutTemplateEntity
import com.example.neokratos.data.local.relations.TemplateWithExerciseDetails
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Template screens.
 */
class TemplateViewModel(
    private val dao: WorkoutTemplateDao
) : ViewModel() {

    /**
     * All templates (basic info only).
     */
    val templates: StateFlow<List<WorkoutTemplateEntity>> = dao.getAllTemplates()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * All templates with full exercise details.
     * Use this for displaying template cards with exercise preview.
     */
    val templatesWithDetails: StateFlow<List<TemplateWithExerciseDetails>> =
        dao.getAllTemplatesWithExerciseDetails()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    /**
     * Create a new empty template.
     */
    fun createTemplate(name: String) {
        viewModelScope.launch {
            dao.insert(WorkoutTemplateEntity(name = name))
        }
    }

    /**
     * Add a template (legacy method for compatibility).
     */
    fun addTemplate(name: String) {
        createTemplate(name)
    }

    /**
     * Delete a template.
     * Cascade delete will also remove all exercises in the template.
     */
    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            dao.deleteById(templateId)
        }
    }

    /**
     * Get template with exercises by ID.
     */
    suspend fun getTemplateWithExercises(templateId: Long): TemplateWithExerciseDetails? {
        return dao.getTemplateWithExerciseDetails(templateId)
    }
}