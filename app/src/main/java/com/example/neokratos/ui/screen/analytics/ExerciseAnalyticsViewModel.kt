package com.example.neokratos.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.entity.ExerciseEntity
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.repository.AnalyticsRepository
import com.example.neokratos.data.repository.ExerciseProgressPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Exercise Analytics screen.
 *
 * Manages analytics for a single selected exercise:
 * - Progress data over time
 * - Personal records
 * - Top sets
 * - Statistics
 */
class ExerciseAnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    // ===== STATE =====

    /**
     * Currently selected exercise.
     */
    private val _selectedExercise = MutableStateFlow<ExerciseEntity?>(null)
    val selectedExercise: StateFlow<ExerciseEntity?> = _selectedExercise.asStateFlow()

    /**
     * Progress data points for the selected exercise.
     */
    private val _progressData = MutableStateFlow<List<ExerciseProgressPoint>>(emptyList())
    val progressData: StateFlow<List<ExerciseProgressPoint>> = _progressData.asStateFlow()

    /**
     * Personal record (best set) for the exercise.
     */
    private val _personalRecord = MutableStateFlow<SetLogEntity?>(null)
    val personalRecord: StateFlow<SetLogEntity?> = _personalRecord.asStateFlow()

    /**
     * Top 10 sets for the exercise.
     */
    private val _topSets = MutableStateFlow<List<SetLogEntity>>(emptyList())
    val topSets: StateFlow<List<SetLogEntity>> = _topSets.asStateFlow()

    // ===== ACTIONS =====

    /**
     * Select an exercise to analyze.
     * Triggers loading of all analytics data for that exercise.
     */
    fun selectExercise(exercise: ExerciseEntity) {
        _selectedExercise.value = exercise
        loadExerciseAnalytics(exercise.id)
    }

    /**
     * Refresh analytics data for current exercise.
     */
    fun refresh() {
        _selectedExercise.value?.let { exercise ->
            loadExerciseAnalytics(exercise.id)
        }
    }

    // ===== DATA LOADING =====

    /**
     * Load all analytics data for an exercise.
     */
    private fun loadExerciseAnalytics(exerciseId: Long) {
        viewModelScope.launch {
            try {
                // Load progress data
                val progress = analyticsRepository.getExerciseProgressData(exerciseId)
                _progressData.value = progress

                // Load personal record
                val pr = analyticsRepository.getPersonalRecord(exerciseId)
                _personalRecord.value = pr

                // Load top sets
                val topSets = analyticsRepository.getTopWorkingSets(exerciseId, limit = 10)
                _topSets.value = topSets

            } catch (e: Exception) {
                // Handle error
                _progressData.value = emptyList()
                _personalRecord.value = null
                _topSets.value = emptyList()
            }
        }
    }
}