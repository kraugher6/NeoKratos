package com.example.neokratos.data.repository

import com.example.neokratos.data.local.dao.WorkoutTemplateDao
import com.example.neokratos.data.local.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

class WorkoutTemplateRepository(
    private val dao: WorkoutTemplateDao
) {

    val allTemplates: Flow<List<WorkoutTemplateEntity>> = dao.getAllTemplates()

    suspend fun insert(template: WorkoutTemplateEntity) {
        dao.insert(template)
    }
}
