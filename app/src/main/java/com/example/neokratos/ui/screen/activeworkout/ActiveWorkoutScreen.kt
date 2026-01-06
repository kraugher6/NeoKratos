package com.example.neokratos.ui.screen.activeworkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neokratos.data.local.entity.getDisplayName
import com.example.neokratos.data.local.entity.getDurationDisplay
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.local.relations.getTotalExercises
import com.example.neokratos.data.local.relations.getTotalSets
import com.example.neokratos.data.local.relations.getTotalVolume

/**
 * Main screen for active workout.
 *
 * Shows:
 * - Workout info (duration, volume)
 * - List of exercises with sets
 * - Add exercise button
 * - Complete/cancel workout buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onNavigateBack: () -> Unit
) {
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedExerciseId by viewModel.selectedExerciseId.collectAsStateWithLifecycle()

    var showExercisePicker by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(activeWorkout?.session?.getDisplayName() ?: "Workout")
                },
                actions = {
                    // Cancel workout button
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel workout")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Complete workout button
                if (activeWorkout != null) {
                    FloatingActionButton(
                        onClick = { showCompleteDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Complete workout")
                    }
                }

                // Add exercise button
                ExtendedFloatingActionButton(
                    onClick = { showExercisePicker = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Exercise") }
                )
            }
        }
    ) { padding ->

        when {
            // No active workout - show start screen
            activeWorkout == null -> {
                EmptyWorkoutState(
                    onStartWorkout = { viewModel.startWorkout() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            // Active workout - show exercises
            else -> {
                ActiveWorkoutContent(
                    session = activeWorkout!!,
                    selectedExerciseId = selectedExerciseId,
                    onExerciseSelected = { viewModel.selectExercise(it) },
                    onAddSet = { sessionExerciseId, weight, reps, rpe ->
                        viewModel.logSetForExercise(
                            sessionExerciseId = sessionExerciseId,
                            weight = weight,
                            reps = reps,
                            rpe = rpe,
                            restSeconds = 90 // Default 90 seconds rest
                        )
                    },
                    onRemoveExercise = { viewModel.removeExercise(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }

        // Loading overlay
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

        // Error snackbar
        if (uiState is WorkoutUiState.Error) {
            val message = (uiState as WorkoutUiState.Error).message
            LaunchedEffect(message) {
                // Show snackbar or toast
            }
        }

        // Workout completed - navigate back
        if (uiState is WorkoutUiState.WorkoutCompleted) {
            LaunchedEffect(Unit) {
                onNavigateBack()
            }
        }
    }

    // Dialogs
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

/**
 * Empty state when no workout is active.
 */
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
            Text("Start Workout")
        }
    }
}

/**
 * Main content showing active workout with exercises.
 */
@Composable
private fun ActiveWorkoutContent(
    session: SessionComplete,
    selectedExerciseId: Long?,
    onExerciseSelected: (Long) -> Unit,
    onAddSet: (sessionExerciseId: Long, weight: Float, reps: Int, rpe: Float?) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {

        // Workout summary card
        WorkoutSummaryCard(
            session = session,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Exercise list
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
                        onAddSet = { weight, reps, rpe ->
                            onAddSet(exerciseWithDetails.sessionExercise.id, weight, reps, rpe)
                        },
                        onRemove = { onRemoveExercise(exerciseWithDetails.sessionExercise.id) }
                    )
                }
            }
        }
    }
}

/**
 * Empty state when no exercises in workout.
 */
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

/**
 * Card showing workout summary (duration, volume, etc.)
 */
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
            // Duration
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

            // Exercises
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

            // Sets
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

            // Volume
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

/**
 * Dialog to confirm completing workout.
 */
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

/**
 * Dialog to confirm canceling workout.
 */
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

/**
 * Simple exercise picker - will use the full version from ExercisePickerDialog.kt
 */
@Composable
private fun ExercisePickerDialog(
    onExerciseSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Import the full ExercisePickerDialog component
    com.example.neokratos.ui.screen.activeworkout.ExercisePickerDialogScreen(
        onExerciseSelected = onExerciseSelected,
        onDismiss = onDismiss
    )
}