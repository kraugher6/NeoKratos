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
 * Active Workout Screen - FIXED VERSION
 *
 * FIX: Removed FAB that overlapped "Add Set" button
 * NOW: Swipe to last page to add exercise (no overlay)
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

    // CHANGE 1: Add +1 page for "Add Exercise" card
    val exerciseCount = activeWorkout?.exercisesWithDetails?.size ?: 0
    val totalPages = if (exerciseCount > 0) exerciseCount + 1 else 0

    val pagerState = rememberPagerState(pageCount = { totalPages })
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

                    if (activeWorkout!!.exercisesWithDetails.isNotEmpty()) {
                        ExerciseProgressBar(
                            exercises = activeWorkout!!.exercisesWithDetails,
                            currentIndex = pagerState.currentPage,
                            totalPages = totalPages,
                            onExerciseClick = { index ->
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        }
        // CHANGE 2: NO floatingActionButton - removed completely!
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    WorkoutSummaryCompact(
                        session = activeWorkout!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // CHANGE 3: Pager now has +1 page for "Add Exercise"
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        pageSpacing = 16.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) { page ->
                        if (page < exerciseCount) {
                            // Normal exercise card
                            val exerciseWithDetails = activeWorkout!!.exercisesWithDetails[page]

                            SwipeableExerciseCardWithTimer(
                                exerciseWithDetails = exerciseWithDetails,
                                exerciseNumber = page + 1,
                                totalExercises = exerciseCount,
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
                        } else {
                            // CHANGE 4: Last page = "Add Exercise" card
                            AddExerciseCard(
                                onAddExercise = { showExercisePicker = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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

// CHANGE 5: NEW - Progress bar with "Add" indicator
@Composable
private fun ExerciseProgressBar(
    exercises: List<SessionExerciseWithDetails>,
    currentIndex: Int,
    totalPages: Int,
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
        val isAddPage = currentIndex >= exercises.size
        Text(
            text = if (isAddPage) "Add Exercise" else "Exercise ${currentIndex + 1} of ${exercises.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            exercises.forEachIndexed { index, exercise ->
                ExerciseProgressDot(
                    isActive = index == currentIndex,
                    isCompleted = exercise.sets.any { it.completed },
                    onClick = { onExerciseClick(index) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Add dot at the end
            AddDot(
                isActive = isAddPage,
                onClick = { onExerciseClick(exercises.size) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ExerciseProgressDot(
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

@Composable
private fun AddDot(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val height by animateFloatAsState(
        targetValue = if (isActive) 8f else 4f,
        label = "add_dot_height"
    )

    Box(
        modifier = modifier
            .height(height.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
    )
}

// CHANGE 6: NEW - "Add Exercise" card shown as last page
@Composable
private fun AddExerciseCard(
    onAddExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Add Exercise",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Swipe here to add another exercise",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onAddExercise,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose Exercise")
            }
        }
    }
}

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
            CompactStat(icon = "⏱️", value = session.session.getDurationDisplay())
            CompactStat(icon = "📊", value = "${session.getTotalSets()}")
            CompactStat(icon = "💪", value = "${session.getTotalVolume().toInt()}kg")
        }
    }
}

@Composable
private fun CompactStat(icon: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = icon, style = MaterialTheme.typography.titleMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
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
        Text(text = "💪", style = MaterialTheme.typography.displayLarge)
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
        Text(text = "🏋️", style = MaterialTheme.typography.displayLarge)
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
            TextButton(onClick = onConfirm) { Text("Complete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
            TextButton(onClick = onDismiss) { Text("No, Continue") }
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
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SwipeableExerciseCard(
            exerciseWithDetails = exerciseWithDetails,
            exerciseNumber = exerciseNumber,
            totalExercises = totalExercises,
            onAddSet = onAddSet,
            onUpdateSet = onUpdateSet,
            onRemoveExercise = onRemoveExercise,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

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
            CompletedTimerContent(onSkip = onSkip, modifier = modifier)
        }
        RestTimerState.Idle -> { /* Hidden */ }
    }
}

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