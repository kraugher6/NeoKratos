package com.example.neokratos.ui.screen.activeworkout

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.repository.ExerciseRepository
import com.example.neokratos.data.repository.WorkoutSessionRepository
import com.example.neokratos.service.RestTimerService
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
 * ✅ FIXED - Proper broadcast synchronization:
 * - Receives broadcasts from RestTimerService
 * - Auto-dismiss after 3 seconds on completion
 * - Coordinated dismiss (app + notification)
 */
class ActiveWorkoutViewModel(
    application: Application,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val exerciseRepository: ExerciseRepository
) : AndroidViewModel(application) {

    val activeWorkout: StateFlow<SessionComplete?> =
        workoutSessionRepository.activeWorkoutComplete
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Idle)
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    val selectedExerciseId: StateFlow<Long?> = _selectedExerciseId.asStateFlow()

    private val _restTimerState = MutableStateFlow<RestTimerState>(RestTimerState.Idle)
    val restTimerState: StateFlow<RestTimerState> = _restTimerState.asStateFlow()

    private var timerJob: Job? = null
    private var autoDismissJob: Job? = null
    private var currentExerciseName = "Exercise"

    /**
     * ✅ FIX 6: Broadcast receiver for service actions
     * Service broadcasts pause/resume/skip → ViewModel receives and acts
     */
    private val serviceActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                RestTimerService.BROADCAST_PAUSE_TIMER -> pauseRestTimer()
                RestTimerService.BROADCAST_RESUME_TIMER -> resumeRestTimer()
                RestTimerService.BROADCAST_SKIP_TIMER -> skipRestTimer()
            }
        }
    }

    init {
        registerBroadcastReceiver()
    }

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
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to start workout")
            }
        }
    }

    fun completeWorkout() {
        viewModelScope.launch {
            try {
                val session = activeWorkout.value ?: return@launch
                _uiState.value = WorkoutUiState.Loading
                stopTimerService()
                workoutSessionRepository.completeWorkout(session.session.id)
                cancelTimer()
                cancelAutoDismiss()
                _restTimerState.value = RestTimerState.Idle
                _uiState.value = WorkoutUiState.WorkoutCompleted
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to complete workout")
            }
        }
    }

    fun cancelWorkout() {
        viewModelScope.launch {
            try {
                val session = activeWorkout.value ?: return@launch
                stopTimerService()
                workoutSessionRepository.cancelWorkout(session.session.id)
                cancelTimer()
                cancelAutoDismiss()
                _restTimerState.value = RestTimerState.Idle
                _uiState.value = WorkoutUiState.Idle
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to cancel workout")
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
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to add exercise")
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
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to remove exercise")
            }
        }
    }

    fun selectExercise(sessionExerciseId: Long) {
        _selectedExerciseId.value = sessionExerciseId
        viewModelScope.launch {
            val workout = activeWorkout.value
            val exercise = workout?.exercisesWithDetails
                ?.find { it.sessionExercise.id == sessionExerciseId }
            currentExerciseName = exercise?.exercise?.name ?: "Exercise"
        }
    }

    fun completeExercise(sessionExerciseId: Long) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.completeExercise(sessionExerciseId)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to complete exercise")
            }
        }
    }

    // ===== SET LOGGING =====

    fun logSetForExercise(sessionExerciseId: Long, weight: Float, reps: Int, rpe: Float? = null, restSeconds: Int) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.logSet(sessionExerciseId, weight, reps, rpe, restSeconds)
                startRestTimer(restSeconds)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to log set")
            }
        }
    }

    fun updateSet(setLog: SetLogEntity) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.updateSet(setLog)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to update set")
            }
        }
    }

    fun updateSetAndStartTimer(setLog: SetLogEntity) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.updateSet(setLog)
                setLog.restSeconds?.let { startRestTimer(it) }
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to update set")
            }
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.deleteSet(setId)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to delete set")
            }
        }
    }

    // ===== REST TIMER =====

    private fun startRestTimer(seconds: Int) {
        cancelTimer()
        cancelAutoDismiss()

        _restTimerState.value = RestTimerState.Running(seconds, seconds)
        RestTimerService.startTimer(getApplication(), currentExerciseName, seconds, seconds)

        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1

                if (_restTimerState.value is RestTimerState.Running) {
                    _restTimerState.value = RestTimerState.Running(seconds, remaining)
                    RestTimerService.updateTimer(getApplication(), currentExerciseName, seconds, remaining, false)
                } else break
            }

            if (remaining == 0) {
                _restTimerState.value = RestTimerState.Completed
                RestTimerService.updateTimer(getApplication(), currentExerciseName, seconds, 0, false)
                startAutoDismiss()
            }
        }
    }

    fun pauseRestTimer() {
        val current = _restTimerState.value
        if (current is RestTimerState.Running) {
            timerJob?.cancel()
            timerJob = null
            _restTimerState.value = RestTimerState.Paused(current.totalSeconds, current.remainingSeconds)
            RestTimerService.updateTimer(getApplication(), currentExerciseName, current.totalSeconds, current.remainingSeconds, true)
        }
    }

    fun resumeRestTimer() {
        val current = _restTimerState.value
        if (current is RestTimerState.Paused) {
            startRestTimer(current.remainingSeconds)
        }
    }

    fun skipRestTimer() {
        cancelTimer()
        cancelAutoDismiss()
        stopTimerService()
        _restTimerState.value = RestTimerState.Idle
    }

    /**
     * ✅ FIX 1 & 2: Auto-dismiss after 3 seconds, coordinated with notification
     */
    private fun startAutoDismiss() {
        autoDismissJob = viewModelScope.launch {
            delay(3000L)
            skipRestTimer()
        }
    }

    private fun cancelAutoDismiss() {
        autoDismissJob?.cancel()
        autoDismissJob = null
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun stopTimerService() {
        RestTimerService.stopService(getApplication())
    }

    private fun registerBroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction(RestTimerService.BROADCAST_PAUSE_TIMER)
            addAction(RestTimerService.BROADCAST_RESUME_TIMER)
            addAction(RestTimerService.BROADCAST_SKIP_TIMER)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(serviceActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                getApplication<Application>(),
                serviceActionReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimer()
        cancelAutoDismiss()
        stopTimerService()
        try {
            getApplication<Application>().unregisterReceiver(serviceActionReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }

    // ===== NOTES =====

    fun updateWorkoutNotes(notes: String) {
        viewModelScope.launch {
            try {
                val session = activeWorkout.value ?: return@launch
                workoutSessionRepository.updateSessionNotes(session.session.id, notes)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to update notes")
            }
        }
    }

    fun updateExerciseNotes(sessionExerciseId: Long, notes: String) {
        viewModelScope.launch {
            try {
                workoutSessionRepository.updateExerciseNotes(sessionExerciseId, notes)
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error(e.message ?: "Failed to update exercise notes")
            }
        }
    }

    fun hasActiveWorkout() = activeWorkout.value != null
    fun getCurrentSessionId() = activeWorkout.value?.session?.id
}

sealed class WorkoutUiState {
    object Idle : WorkoutUiState()
    object Loading : WorkoutUiState()
    data class WorkoutActive(val sessionId: Long) : WorkoutUiState()
    object WorkoutCompleted : WorkoutUiState()
    data class Error(val message: String) : WorkoutUiState()
}

sealed class RestTimerState {
    object Idle : RestTimerState()
    data class Running(val totalSeconds: Int, val remainingSeconds: Int) : RestTimerState()
    data class Paused(val totalSeconds: Int, val remainingSeconds: Int) : RestTimerState()
    object Completed : RestTimerState()
}