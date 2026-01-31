package com.example.neokratos.ui.screen.activeworkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neokratos.data.local.entity.getDisplayName
import com.example.neokratos.data.local.entity.getDurationDisplay
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.local.relations.SessionExerciseWithDetails
import com.example.neokratos.data.local.relations.getTotalSets
import com.example.neokratos.data.local.relations.getTotalVolume
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Active Workout Screen - REFACTORED for better UX
 *
 * Changes:
 * - Rest timer INTEGRATED into exercise card (no overlay)
 * - "Complete Workout" moved to TopAppBar
 * - "Add Exercise" FAB only visible on last page
 * - Cleaner, less cluttered layout
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    // Pager state for horizontal swiping
    val pagerState = rememberPagerState(pageCount = {
        activeWorkout?.exercisesWithDetails?.size ?: 0
    })
    val scope = rememberCoroutineScope()

    // Sync pager position with selected exercise
    LaunchedEffect(selectedExerciseId, activeWorkout) {
        if (selectedExerciseId != null && activeWorkout != null) {
            val index = activeWorkout!!.exercisesWithDetails
                .indexOfFirst { it.sessionExercise.id == selectedExerciseId }
            if (index >= 0 && index != pagerState.currentPage) {
                pagerState.animateScrollToPage(index)
            }
        }
    }

    // Update selected exercise when pager changes
    LaunchedEffect(pagerState.currentPage, activeWorkout) {
        if (activeWorkout != null && activeWorkout!!.exercisesWithDetails.isNotEmpty()) {
            val currentExercise = activeWorkout!!.exercisesWithDetails
                .getOrNull(pagerState.currentPage)
            currentExercise?.let {
                viewModel.selectExercise(it.sessionExercise.id)
            }
        }
    }

    Scaffold(
        topBar = {
            if (activeWorkout != null) {
                Column {
                    TopAppBar(
                        title = {
                            Text(activeWorkout?.session?.getDisplayName() ?: "")
                        },
                        navigationIcon = {
                            IconButton(onClick = { showCancelDialog = true }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel workout")
                            }
                        },
                        actions = {
                            // MOVED: Complete button to TopAppBar
                            IconButton(
                                onClick = { showCompleteDialog = true }
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Complete workout",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    // Progress bar
                    if (activeWorkout!!.exercisesWithDetails.isNotEmpty()) {
                        ExerciseProgressBar(
                            exercises = activeWorkout!!.exercisesWithDetails,
                            currentIndex = pagerState.currentPage,
                            onExerciseClick = { index ->
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // NEW: Only show FAB when on last page OR workout is empty
            val isLastPage = activeWorkout?.let {
                it.exercisesWithDetails.isEmpty() ||
                        pagerState.currentPage == it.exercisesWithDetails.size - 1
            } ?: true

            AnimatedVisibility(
                visible = activeWorkout != null && isLastPage,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showExercisePicker = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Exercise") }
                )
            }
        }
    ) { padding ->

        when {
            activeWorkout == null -> {
                EmptyWorkoutState(
                    onStartWorkout = { viewModel.startWorkout() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            activeWorkout!!.exercisesWithDetails.isEmpty() -> {
                EmptyExerciseListState(
                    onAddExercise = { showExercisePicker = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            else -> {
                // HORIZONTAL PAGER - Swipeable cards
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Workout summary at top
                    WorkoutSummaryCompact(
                        session = activeWorkout!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Swipeable exercise cards
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        pageSpacing = 16.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) { page ->
                        val exerciseWithDetails = activeWorkout!!.exercisesWithDetails[page]

                        // NEW: Pass rest timer state to card
                        SwipeableExerciseCardWithTimer(
                            exerciseWithDetails = exerciseWithDetails,
                            exerciseNumber = page + 1,
                            totalExercises = activeWorkout!!.exercisesWithDetails.size,
                            restTimerState = restTimerState,
                            onAddSet = { weight, reps, rpe, restSeconds ->
                                viewModel.logSetForExercise(
                                    sessionExerciseId = exerciseWithDetails.sessionExercise.id,
                                    weight = weight,
                                    reps = reps,
                                    rpe = rpe,
                                    restSeconds = restSeconds
                                )
                            },
                            onUpdateSet = { updatedSet ->
                                viewModel.updateSetAndStartTimer(updatedSet)
                            },
                            onRemoveExercise = {
                                viewModel.removeExercise(exerciseWithDetails.sessionExercise.id)
                            },
                            onPauseTimer = { viewModel.pauseRestTimer() },
                            onResumeTimer = { viewModel.resumeRestTimer() },
                            onSkipTimer = { viewModel.skipRestTimer() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
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
 * Interactive progress bar showing all exercises.
 * Tap on dot to jump to that exercise.
 */
@Composable
private fun ExerciseProgressBar(
    exercises: List<SessionExerciseWithDetails>,
    currentIndex: Int,
    onExerciseClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Progress text
        Text(
            text = "Exercise ${currentIndex + 1} of ${exercises.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Progress dots row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            exercises.forEachIndexed { index, exercise ->
                ExerciseProgressDot(
                    exerciseName = exercise.exercise.name,
                    isActive = index == currentIndex,
                    isCompleted = exercise.sets.any { it.completed },
                    onClick = { onExerciseClick(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Single dot in progress bar.
 */
@Composable
private fun ExerciseProgressDot(
    exerciseName: String,
    isActive: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isCompleted -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val height by animateFloatAsState(
        targetValue = if (isActive) 8f else 4f,
        label = "dot_height"
    )

    Box(
        modifier = modifier
            .height(height.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
    )
}

/**
 * Compact workout summary - ONE line with key stats.
 */
@Composable
private fun WorkoutSummaryCompact(
    session: SessionComplete,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactStat(
                icon = "⏱️",
                value = session.session.getDurationDisplay()
            )
            CompactStat(
                icon = "📊",
                value = "${session.getTotalSets()}"
            )
            CompactStat(
                icon = "💪",
                value = "${session.getTotalVolume().toInt()}kg"
            )
        }
    }
}

@Composable
private fun CompactStat(
    icon: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Empty state when no workout active.
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
            text = "💪",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Active Workout",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start tracking your lifts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartWorkout,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Start Empty Workout")
        }
    }
}

/**
 * Empty state when workout has no exercises.
 */
@Composable
private fun EmptyExerciseListState(
    onAddExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏋️",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Exercises Yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add exercises to start logging sets",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddExercise,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Exercise")
        }
    }
}

/**
 * Complete workout dialog.
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
 * Cancel workout dialog.
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
 * Exercise picker dialog.
 */
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

/**
 * NEW: Swipeable Exercise Card WITH integrated timer
 *
 * Timer is shown at the bottom of the card when active.
 * This eliminates the overlay and uses card's own space.
 */
@Composable
private fun SwipeableExerciseCardWithTimer(
    exerciseWithDetails: SessionExerciseWithDetails,
    exerciseNumber: Int,
    totalExercises: Int,
    restTimerState: RestTimerState,
    onAddSet: (weight: Float, reps: Int, rpe: Float?, restSeconds: Int) -> Unit,
    onUpdateSet: (com.example.neokratos.data.local.entity.SetLogEntity) -> Unit,
    onRemoveExercise: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onSkipTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use the existing SwipeableExerciseCard but add timer section
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main exercise card (without integrated timer controls yet)
        SwipeableExerciseCard(
            exerciseWithDetails = exerciseWithDetails,
            exerciseNumber = exerciseNumber,
            totalExercises = totalExercises,
            onAddSet = onAddSet,
            onUpdateSet = onUpdateSet,
            onRemoveExercise = onRemoveExercise,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // INTEGRATED TIMER at bottom (only when active)
        AnimatedVisibility(
            visible = restTimerState !is RestTimerState.Idle,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            IntegratedRestTimer(
                restTimerState = restTimerState,
                onPause = onPauseTimer,
                onResume = onResumeTimer,
                onSkip = onSkipTimer,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Integrated rest timer component.
 * Shows at bottom of exercise card instead of overlay.
 */
@Composable
private fun IntegratedRestTimer(
    restTimerState: RestTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (restTimerState) {
        is RestTimerState.Running -> {
            RestTimerContent(
                totalSeconds = restTimerState.totalSeconds,
                remainingSeconds = restTimerState.remainingSeconds,
                isRunning = true,
                onPause = onPause,
                onResume = onResume,
                onSkip = onSkip,
                modifier = modifier
            )
        }
        is RestTimerState.Paused -> {
            RestTimerContent(
                totalSeconds = restTimerState.totalSeconds,
                remainingSeconds = restTimerState.remainingSeconds,
                isRunning = false,
                onPause = onPause,
                onResume = onResume,
                onSkip = onSkip,
                modifier = modifier
            )
        }
        is RestTimerState.Completed -> {
            CompletedTimerContent(
                onSkip = onSkip,
                modifier = modifier
            )
        }
        RestTimerState.Idle -> { /* Hidden */ }
    }
}

/**
 * Timer content (running or paused).
 */
@Composable
private fun RestTimerContent(
    totalSeconds: Int,
    remainingSeconds: Int,
    isRunning: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remainingSeconds.toFloat() / totalSeconds.toFloat()
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isRunning) "Rest Timer" else "Rest Timer (Paused)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Progress ring with time
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
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

/**
 * Completed timer content.
 */
@Composable
private fun CompletedTimerContent(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "✓ Rest Complete!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Ready for next set",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Button(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dismiss")
            }
        }
    }
}