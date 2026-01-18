package com.example.neokratos.ui.screen.activeworkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.repository.ExerciseRepository
import com.example.neokratos.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Active Workout screen.
 *
 * FIXED: Automatic rest timer start after logging sets
 */
class ActiveWorkoutViewModel(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    // ===== STATE =====

    val activeWorkout: StateFlow<SessionComplete?> =
        workoutSessionRepository.activeWorkoutComplete
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Idle)
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    val selectedExerciseId: StateFlow<Long?> = _selectedExerciseId.asStateFlow()

    /**
     * Rest timer state with proper coroutine management.
     */
    private val _restTimerState = MutableStateFlow<RestTimerState>(RestTimerState.Idle)
    val restTimerState: StateFlow<RestTimerState> = _restTimerState.asStateFlow()

    /**
     * Job for timer coroutine - allows cancellation.
     */
    private var timerJob: Job? = null

    // ===== WORKOUT OPERATIONS =====

    fun startWorkout(templateId: Long? = null, name: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = WorkoutUiState.Loading

                val sessionId = if (templateId != null) {
                    workoutSessionRepository.startWorkoutFromTemplate(templateId)
                } else {
                    workoutSessionRepository.startWorkout(name = name)
                }

                _uiState.value = WorkoutUiState.WorkoutActive(sessionId)

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to start workout"
                )
            }
        }
    }

    fun completeWorkout() {
        viewModelScope.launch {
            try {
                val session = activeWorkout.value ?: return@launch

                _uiState.value = WorkoutUiState.Loading

                workoutSessionRepository.completeWorkout(session.session.id)

                // FIX: Cancel timer and reset state
                cancelTimer()
                _restTimerState.value = RestTimerState.Idle

                _uiState.value = WorkoutUiState.WorkoutCompleted

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to complete workout"
                )
            }
        }
    }

    fun cancelWorkout() {
        viewModelScope.launch {
            try {
                val session = activeWorkout.value ?: return@launch

                workoutSessionRepository.cancelWorkout(session.session.id)

                // FIX: Cancel timer and reset state
                cancelTimer()
                _restTimerState.value = RestTimerState.Idle

                _uiState.value = WorkoutUiState.Idle

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to cancel workout"
                )
            }
        }
    }

    // ===== EXERCISE OPERATIONS =====

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            try {
                val session = activeWorkout.value ?: return@launch

                val sessionExerciseId = workoutSessionRepository.addExerciseToSession(
                    sessionId = session.session.id,
                    exerciseId = exerciseId
                )

                _selectedExerciseId.value = sessionExerciseId

                exerciseRepository.incrementUsageCount(exerciseId)

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to add exercise"
                )
            }
        }
    }

    fun removeExercise(sessionExerciseId: Long) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.removeExerciseFromSession(sessionExerciseId)

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

    fun selectExercise(sessionExerciseId: Long) {
        _selectedExerciseId.value = sessionExerciseId
    }

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

                // FIX: Start rest timer if restSeconds specified
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
     * Log a set for a specific exercise.
     *
     * FIX: Automatically starts rest timer after logging the set.
     */
    fun logSetForExercise(
        sessionExerciseId: Long,
        weight: Float,
        reps: Int,
        rpe: Float? = null,
        restSeconds: Int
    ) {
        viewModelScope.launch {
            try {
                // Log the set in database
                workoutSessionRepository.logSet(
                    sessionExerciseId = sessionExerciseId,
                    weight = weight,
                    reps = reps,
                    rpe = rpe,
                    restSeconds = restSeconds
                )

                // FIX: Automatically start rest timer with the restSeconds from the set
                // This happens AFTER the set is successfully logged
                startRestTimer(restSeconds)

            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(
                    e.message ?: "Failed to log set"
                )
            }
        }
    }

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

    // ===== REST TIMER - IMPLEMENTATION =====

    /**
     * Start rest timer with coroutine-based countdown.
     *
     * FIX: Now called automatically after logging a set.
     *
     * Concept:
     * - Launches coroutine in viewModelScope
     * - Uses delay(1000L) for 1-second intervals
     * - Updates state each second
     * - Job stored for cancellation
     */
    private fun startRestTimer(seconds: Int) {
        // Cancel existing timer if running
        cancelTimer()

        // Set initial state
        _restTimerState.value = RestTimerState.Running(
            totalSeconds = seconds,
            remainingSeconds = seconds
        )

        // Launch countdown coroutine
        timerJob = viewModelScope.launch {
            var remaining = seconds

            while (remaining > 0) {
                delay(1000L) // Wait 1 second
                remaining -= 1

                // Update state if still running (not paused)
                if (_restTimerState.value is RestTimerState.Running) {
                    _restTimerState.value = RestTimerState.Running(
                        totalSeconds = seconds,
                        remainingSeconds = remaining
                    )
                } else {
                    // Timer was paused, exit loop
                    break
                }
            }

            // Timer completed
            if (remaining == 0) {
                _restTimerState.value = RestTimerState.Completed
                // TODO: Play sound/vibrate notification
            }
        }
    }

    /**
     * Pause rest timer.
     * Cancels coroutine and saves current state.
     */
    fun pauseRestTimer() {
        val current = _restTimerState.value
        if (current is RestTimerState.Running) {
            // Cancel coroutine
            timerJob?.cancel()
            timerJob = null

            // Save paused state
            _restTimerState.value = RestTimerState.Paused(
                totalSeconds = current.totalSeconds,
                remainingSeconds = current.remainingSeconds
            )
        }
    }

    /**
     * Resume rest timer.
     * Restarts coroutine from paused time.
     */
    fun resumeRestTimer() {
        val current = _restTimerState.value
        if (current is RestTimerState.Paused) {
            // Restart timer from remaining time
            startRestTimer(current.remainingSeconds)
        }
    }

    /**
     * Skip/cancel rest timer.
     */
    fun skipRestTimer() {
        cancelTimer()
        _restTimerState.value = RestTimerState.Idle
    }

    /**
     * Cancel timer coroutine.
     */
    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Cleanup when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        cancelTimer()
    }

    // ===== NOTES =====

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

    fun hasActiveWorkout(): Boolean {
        return activeWorkout.value != null
    }

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