package com.example.neokratos.ui.screen.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.dao.TemplateExerciseDao
import com.example.neokratos.data.local.dao.WorkoutTemplateDao
import com.example.neokratos.data.local.entity.TemplateExerciseEntity
import com.example.neokratos.data.local.relations.TemplateWithExerciseDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for editing templates.
 */
class TemplateEditViewModel(
    private val templateDao: WorkoutTemplateDao,
    private val templateExerciseDao: TemplateExerciseDao
) : ViewModel() {

    private var currentTemplateId: Long? = null

    /**
     * Load template for editing.
     */
    fun loadTemplate(templateId: Long) {
        currentTemplateId = templateId
    }

    /**
     * Get template with exercises as Flow.
     */
    fun getTemplateWithExercises(templateId: Long): Flow<TemplateWithExerciseDetails?> {
        return templateDao.getTemplateWithExerciseDetailsFlow(templateId)
    }

    /**
     * Add an exercise to the template.
     */
    fun addExerciseToTemplate(templateId: Long, exerciseId: Long) {
        viewModelScope.launch {
            // Get current exercise count to set order
            val currentCount = templateExerciseDao.getExerciseCount(templateId)

            val templateExercise = TemplateExerciseEntity(
                templateId = templateId,
                exerciseId = exerciseId,
                order = currentCount,
                targetSets = 3,
                targetRepsMin = 8,
                targetRepsMax = 12
            )

            templateExerciseDao.insert(templateExercise)
        }
    }

    /**
     * Remove an exercise from the template.
     */
    fun removeExerciseFromTemplate(templateExerciseId: Long) {
        viewModelScope.launch {
            templateExerciseDao.deleteById(templateExerciseId)
        }
    }

    /**
     * Update exercise targets (sets, reps).
     */
    fun updateTemplateExercise(exercise: TemplateExerciseEntity) {
        viewModelScope.launch {
            templateExerciseDao.update(exercise)
        }
    }
}