package com.example.neokratos.ui.screen.activeworkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.repository.ExerciseRepository
import com.example.neokratos.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Active Workout screen.
 *
 * Manages state and operations during an active workout:
 * - Starting/ending workout
 * - Adding/removing exercises
 * - Logging sets
 * - Timer for rest periods
 *
 * This is the main ViewModel for Release 1.0 live workout feature.
 */
class ActiveWorkoutViewModel(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    // ===== STATE =====

    /**
     * Currently active workout with all details.
     * null if no workout is active.
     *
     * UI observes this to display exercises and sets.
     */
    val activeWorkout: StateFlow<SessionComplete?> =
        workoutSessionRepository.activeWorkoutComplete
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    /**
     * UI state for loading, errors, etc.
     */
    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Idle)
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    /**
     * Currently selected exercise for adding sets.
     * Used in UI to show which exercise is "active" for logging.
     */
    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    val selectedExerciseId: StateFlow<Long?> = _selectedExerciseId.asStateFlow()

    /**
     * Rest timer state.
     * When user logs a set, timer starts automatically.
     */
    private val _restTimerState = MutableStateFlow<RestTimerState>(RestTimerState.Idle)
    val restTimerState: StateFlow<RestTimerState> = _restTimerState.asStateFlow()

    // ===== WORKOUT OPERATIONS =====

    /**
     * Start a new workout session.
     *
     * @param templateId Optional template to base workout on
     * @param name Optional custom name
     */
    fun startWorkout(templateId: Long? = null, name: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = WorkoutUiState.Loading

                val sessionId = workoutSessionRepository.startWorkout(
                    templateId = templateId,
                    name = name
                )

                _uiState.value = WorkoutUiState.WorkoutActive(sessionId)

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to start workout"
                )
            }
        }
    }

    /**
     * Complete the active workout.
     * Calculates final stats and marks as finished.
     */
    fun completeWorkout() {
        viewModelScope.launch {
            try {
                val session = activeWorkout.value ?: return@launch

                _uiState.value = WorkoutUiState.Loading

                workoutSessionRepository.completeWorkout(session.session.id)

                _uiState.value = WorkoutUiState.WorkoutCompleted

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to complete workout"
                )
            }
        }
    }

    /**
     * Cancel the active workout.
     * Deletes the session and all its data.
     */
    fun cancelWorkout() {
        viewModelScope.launch {
            try {
                val session = activeWorkout.value ?: return@launch

                workoutSessionRepository.cancelWorkout(session.session.id)

                _uiState.value = WorkoutUiState.Idle

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to cancel workout"
                )
            }
        }
    }

    // ===== EXERCISE OPERATIONS =====

    /**
     * Add an exercise to the current workout.
     *
     * @param exerciseId Exercise from library
     */
    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            try {
                val session = activeWorkout.value ?: return@launch

                val sessionExerciseId = workoutSessionRepository.addExerciseToSession(
                    sessionId = session.session.id,
                    exerciseId = exerciseId
                )

                // Auto-select the newly added exercise
                _selectedExerciseId.value = sessionExerciseId

                // Increment usage count for this exercise
                exerciseRepository.incrementUsageCount(exerciseId)

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to add exercise"
                )
            }
        }
    }

    /**
     * Remove an exercise from the workout.
     */
    fun removeExercise(sessionExerciseId: Long) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.removeExerciseFromSession(sessionExerciseId)

                // Clear selection if this was the selected exercise
                if (_selectedExerciseId.value == sessionExerciseId) {
                    _selectedExerciseId.value = null
                }

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to remove exercise"
                )
            }
        }
    }

    /**
     * Select an exercise to add sets to.
     * Used in UI to highlight which exercise is "active".
     */
    fun selectExercise(sessionExerciseId: Long) {
        _selectedExerciseId.value = sessionExerciseId
    }

    /**
     * Mark an exercise as completed.
     */
    fun completeExercise(sessionExerciseId: Long) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.completeExercise(sessionExerciseId)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to complete exercise"
                )
            }
        }
    }

    // ===== SET LOGGING =====

    /**
     * Log a set for the currently selected exercise.
     *
     * This is called when user taps "Add Set" button.
     *
     * @param weight Weight in kg
     * @param reps Number of reps
     * @param rpe Rate of Perceived Exertion (1-10)
     * @param restSeconds Rest time after this set
     */
    fun logSet(
        weight: Float,
        reps: Int,
        rpe: Float? = null,
        restSeconds: Int? = null
    ) {
        viewModelScope.launch {
            try {
                val sessionExerciseId = _selectedExerciseId.value
                    ?: throw IllegalStateException("No exercise selected")

                workoutSessionRepository.logSet(
                    sessionExerciseId = sessionExerciseId,
                    weight = weight,
                    reps = reps,
                    rpe = rpe,
                    restSeconds = restSeconds
                )

                // Start rest timer if restSeconds specified
                restSeconds?.let {
                    startRestTimer(it)
                }

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to log set"
                )
            }
        }
    }

    /**
     * Log a set for a specific exercise (not the selected one).
     * Useful when editing sets from exercise list.
     */
    fun logSetForExercise(
        sessionExerciseId: Long,
        weight: Float,
        reps: Int,
        rpe: Float? = null,
        restSeconds: Int? = null
    ) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.logSet(
                    sessionExerciseId = sessionExerciseId,
                    weight = weight,
                    reps = reps,
                    rpe = rpe,
                    restSeconds = restSeconds
                )

                restSeconds?.let {
                    startRestTimer(it)
                }

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to log set"
                )
            }
        }
    }

    /**
     * Update an existing set.
     */
    fun updateSet(setLog: SetLogEntity) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.updateSet(setLog)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to update set"
                )
            }
        }
    }

    /**
     * Delete a set.
     */
    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.deleteSet(setId)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to delete set"
                )
            }
        }
    }

    // ===== REST TIMER =====

    /**
     * Start rest timer for N seconds.
     * Timer counts down and notifies when complete.
     */
    private fun startRestTimer(seconds: Int) {
        // Simple implementation - will be enhanced with actual countdown later
        _restTimerState.value = RestTimerState.Running(
            totalSeconds = seconds,
            remainingSeconds = seconds
        )
    }

    /**
     * Pause rest timer.
     */
    fun pauseRestTimer() {
        val current = _restTimerState.value
        if (current is RestTimerState.Running) {
            _restTimerState.value = RestTimerState.Paused(
                totalSeconds = current.totalSeconds,
                remainingSeconds = current.remainingSeconds
            )
        }
    }

    /**
     * Resume rest timer.
     */
    fun resumeRestTimer() {
        val current = _restTimerState.value
        if (current is RestTimerState.Paused) {
            _restTimerState.value = RestTimerState.Running(
                totalSeconds = current.totalSeconds,
                remainingSeconds = current.remainingSeconds
            )
        }
    }

    /**
     * Skip/cancel rest timer.
     */
    fun skipRestTimer() {
        _restTimerState.value = RestTimerState.Idle
    }

    // ===== NOTES =====

    /**
     * Update workout notes.
     */
    fun updateWorkoutNotes(notes: String) {
        viewModelScope.launch {
            try {
                val session = activeWorkout.value ?: return@launch
                workoutSessionRepository.updateSessionNotes(session.session.id, notes)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to update notes"
                )
            }
        }
    }

    /**
     * Update exercise notes.
     */
    fun updateExerciseNotes(sessionExerciseId: Long, notes: String) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.updateExerciseNotes(sessionExerciseId, notes)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to update exercise notes"
                )
            }
        }
    }

    // ===== HELPERS =====

    /**
     * Check if there's an active workout.
     */
    fun hasActiveWorkout(): Boolean {
        return activeWorkout.value != null
    }

    /**
     * Get current session ID.
     */
    fun getCurrentSessionId(): Long? {
        return activeWorkout.value?.session?.id
    }
}

/**
 * UI state for the workout screen.
 */
sealed class WorkoutUiState {
    object Idle : WorkoutUiState()
    object Loading : WorkoutUiState()
    data class WorkoutActive(val sessionId: Long) : WorkoutUiState()
    object WorkoutCompleted : WorkoutUiState()
    data class Error(val message: String) : WorkoutUiState()
}

/**
 * State for rest timer between sets.
 */
sealed class RestTimerState {
    object Idle : RestTimerState()
    data class Running(
        val totalSeconds: Int,
        val remainingSeconds: Int
    ) : RestTimerState()
    data class Paused(
        val totalSeconds: Int,
        val remainingSeconds: Int
    ) : RestTimerState()
    object Completed : RestTimerState()
}