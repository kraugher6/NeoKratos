package com.example.neokratos.ui.screen.activeworkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.entity.getDisplayName
import com.example.neokratos.data.local.entity.getDurationDisplay
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.local.relations.getTotalExercises
import com.example.neokratos.data.local.relations.getTotalSets
import com.example.neokratos.data.local.relations.getTotalVolume
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onNavigateBack: () -> Unit
) {
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedExerciseId by viewModel.selectedExerciseId.collectAsStateWithLifecycle()
    val restTimerState by viewModel.restTimerState.collectAsStateWithLifecycle()

    var showExercisePicker by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (activeWorkout != null) {
                TopAppBar(
                    title = {
                        Text(activeWorkout?.session?.getDisplayName() ?: "")
                    },
                    actions = {
                        IconButton(onClick = { showCancelDialog = true }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel workout")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (activeWorkout != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showCompleteDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Complete workout")
                    }

                    ExtendedFloatingActionButton(
                        onClick = { showExercisePicker = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Add Exercise") }
                    )
                }
            }
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                activeWorkout == null -> {
                    EmptyWorkoutState(
                        onStartWorkout = { viewModel.startWorkout() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }

                else -> {
                    ActiveWorkoutContent(
                        session = activeWorkout!!,
                        selectedExerciseId = selectedExerciseId,
                        onExerciseSelected = { viewModel.selectExercise(it) },
                        onAddSet = { sessionExerciseId, weight, reps, rpe, restSeconds ->
                            viewModel.logSetForExercise(
                                sessionExerciseId = sessionExerciseId,
                                weight = weight,
                                reps = reps,
                                rpe = rpe,
                                restSeconds = restSeconds
                            )
                        },
                        onRemoveExercise = { viewModel.removeExercise(it) },
                        onUpdateSet = { updatedSet ->
                            viewModel.updateSet(updatedSet)
                        },
                        onStartTimer = { restSeconds ->
                            viewModel.startTimerManually(restSeconds)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
            }

            // Rest Timer Overlay
            if (restTimerState is RestTimerState.Running || restTimerState is RestTimerState.Paused) {
                RestTimerOverlay(
                    restTimerState = restTimerState,
                    onPause = { viewModel.pauseRestTimer() },
                    onResume = { viewModel.resumeRestTimer() },
                    onSkip = { viewModel.skipRestTimer() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                )
            }

            if (uiState is WorkoutUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (uiState is WorkoutUiState.Error) {
            val message = (uiState as WorkoutUiState.Error).message
            LaunchedEffect(message) {
                // Handle error
            }
        }

        if (uiState is WorkoutUiState.WorkoutCompleted) {
            LaunchedEffect(Unit) {
                onNavigateBack()
            }
        }
    }

    if (showCompleteDialog) {
        CompleteWorkoutDialog(
            onConfirm = {
                viewModel.completeWorkout()
                showCompleteDialog = false
            },
            onDismiss = { showCompleteDialog = false }
        )
    }

    if (showCancelDialog) {
        CancelWorkoutDialog(
            onConfirm = {
                viewModel.cancelWorkout()
                showCancelDialog = false
                onNavigateBack()
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            onExerciseSelected = { exerciseId ->
                viewModel.addExercise(exerciseId)
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false }
        )
    }
}

@Composable
private fun RestTimerOverlay(
    restTimerState: RestTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (totalSeconds, remainingSeconds, isRunning) = when (restTimerState) {
        is RestTimerState.Running -> Triple(
            restTimerState.totalSeconds,
            restTimerState.remainingSeconds,
            true
        )
        is RestTimerState.Paused -> Triple(
            restTimerState.totalSeconds,
            restTimerState.remainingSeconds,
            false
        )
        else -> return
    }

    val progress = remainingSeconds.toFloat() / totalSeconds.toFloat()
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rest Timer",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = if (isRunning) onPause else onResume,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Resume"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRunning) "Pause" else "Resume")
                }

                Button(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkoutState(
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Active Workout",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start a new workout to begin tracking",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onStartWorkout) {
            Text("Start Empty Workout")
        }
    }
}

@Composable
private fun ActiveWorkoutContent(
    session: SessionComplete,
    selectedExerciseId: Long?,
    onExerciseSelected: (Long) -> Unit,
    onAddSet: (sessionExerciseId: Long, weight: Float, reps: Int, rpe: Float?, restSeconds: Int) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onUpdateSet: (SetLogEntity) -> Unit,
    onStartTimer: (restSeconds: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        WorkoutSummaryCard(
            session = session,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (session.exercisesWithDetails.isEmpty()) {
            EmptyExerciseList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = session.exercisesWithDetails,
                    key = { it.sessionExercise.id }
                ) { exerciseWithDetails ->
                    ExerciseCard(
                        exerciseWithDetails = exerciseWithDetails,
                        isSelected = exerciseWithDetails.sessionExercise.id == selectedExerciseId,
                        onSelect = { onExerciseSelected(exerciseWithDetails.sessionExercise.id) },
                        onAddSet = { weight, reps, rpe, restSeconds ->
                            onAddSet(exerciseWithDetails.sessionExercise.id, weight, reps, rpe, restSeconds)
                        },
                        onRemove = { onRemoveExercise(exerciseWithDetails.sessionExercise.id) },
                        onUpdateSet = { updatedSet ->
                            onUpdateSet(updatedSet)
                        },
                        onStartTimer = onStartTimer
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyExerciseList(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No exercises yet",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Tap the + button to add exercises",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkoutSummaryCard(
    session: SessionComplete,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = session.session.getDurationDisplay(),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${session.getTotalExercises()}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Sets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${session.getTotalSets()}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Volume",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${session.getTotalVolume().toInt()} kg",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun CompleteWorkoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Workout?") },
        text = { Text("Are you sure you want to finish this workout?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Complete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CancelWorkoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Workout?") },
        text = { Text("All progress will be lost. Are you sure?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes, Cancel", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No, Continue")
            }
        }
    )
}

@Composable
private fun ExercisePickerDialog(
    onExerciseSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ExercisePickerDialogScreen(
        onExerciseSelected = onExerciseSelected,
        onDismiss = onDismiss
    )
}