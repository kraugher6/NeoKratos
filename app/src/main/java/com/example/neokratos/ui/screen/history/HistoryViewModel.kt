package com.example.neokratos.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for History screen.
 */
class HistoryViewModel(
    private val repository: WorkoutSessionRepository
) : ViewModel() {

    /**
     * All completed workouts with full details.
     */
    val workouts = repository.completedWorkoutsComplete
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * Get detailed workout by ID.
     */
    fun getWorkoutDetail(sessionId: Long): Flow<SessionComplete?> {
        return repository.getSessionCompleteFlow(sessionId)
    }
}