package com.example.neokratos.ui.screen.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neokratos.data.local.entity.ExerciseEntity
import com.example.neokratos.data.local.entity.estimateOneRepMax
import com.example.neokratos.data.local.entity.getVolume
import com.example.neokratos.data.repository.ExerciseProgressPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for analyzing progress on a specific exercise.
 *
 * Shows:
 * - Exercise selector
 * - Progress graphs (weight, volume, 1RM over time)
 * - Personal records
 * - Statistics summary
 *
 * Concepts:
 * - Exercise-specific analytics
 * - Time-series data visualization
 * - PR tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseAnalyticsScreen(
    viewModel: ExerciseAnalyticsViewModel,
    onBack: () -> Unit
) {
    val selectedExercise by viewModel.selectedExercise.collectAsStateWithLifecycle()
    val progressData by viewModel.progressData.collectAsStateWithLifecycle()
    val personalRecord by viewModel.personalRecord.collectAsStateWithLifecycle()
    val topSets by viewModel.topSets.collectAsStateWithLifecycle()

    var showExercisePicker by remember { mutableStateOf(selectedExercise == null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedExercise?.name ?: "Exercise Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExercisePicker = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Select Exercise")
                    }
                }
            )
        }
    ) { padding ->
        if (selectedExercise == null) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select an Exercise",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Button(onClick = { showExercisePicker = true }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Exercise")
                    }
                }
            }
        } else {
            // Show analytics
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Personal Record Section
                item {
                    Text(
                        text = "Personal Record",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    PersonalRecordCard(pr = personalRecord)
                }

                // Progress Graph Section
                item {
                    Text(
                        text = "Progress Over Time",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    ProgressGraphCard(data = progressData)
                }

                // Top Sets Section
                item {
                    Text(
                        text = "Top 10 Sets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(topSets) { set ->
                    TopSetCard(set = set)
                }
            }
        }
    }

    // Exercise picker dialog
    if (showExercisePicker) {
        ExercisePickerDialog(
            onExerciseSelected = { exercise ->
                viewModel.selectExercise(exercise)
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false }
        )
    }
}

/**
 * Card displaying personal record.
 */
@Composable
private fun PersonalRecordCard(
    pr: com.example.neokratos.data.local.entity.SetLogEntity?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pr == null) {
                Text(
                    text = "No data yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Best Set",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${pr.weight} kg × ${pr.reps} reps",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Est. 1RM",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${pr.estimateOneRepMax().toInt()} kg",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (pr.rpe != null) {
                    Text(
                        text = "RPE: ${String.format("%.1f", pr.rpe)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = formatDate(pr.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Card displaying progress graph.
 * Shows simple data points for now, will add actual chart later.
 */
@Composable
private fun ProgressGraphCard(
    data: List<ExerciseProgressPoint>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Weight Progression",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (data.isEmpty()) {
                Text(
                    text = "No workout data yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Show last 10 data points
                data.takeLast(10).forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = formatDate(point.timestamp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${point.totalSets} sets • Avg RPE ${String.format("%.1f", point.avgRPE)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${point.maxWeight} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${point.totalVolume.toInt()} kg vol",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Divider()
                }
            }

            // TODO: Add actual chart here
            Text(
                text = "📈 Chart visualization coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Card displaying a single top set.
 */
@Composable
private fun TopSetCard(
    set: com.example.neokratos.data.local.entity.SetLogEntity,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${set.weight} kg × ${set.reps} reps",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = formatDate(set.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (set.rpe != null) {
                        Text(
                            text = "• RPE ${String.format("%.1f", set.rpe)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${set.getVolume().toInt()} kg",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "volume",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dialog for selecting an exercise.
 */
@Composable
private fun ExercisePickerDialog(
    onExerciseSelected: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    // Use the existing ExercisePickerDialogScreen
    // But adapted to return ExerciseEntity instead of Long
    com.example.neokratos.ui.screen.activeworkout.ExercisePickerDialogScreen(
        onExerciseSelected = { exerciseId ->
            // For now, we'll need to create a simple version
            // In real implementation, pass the full exercise object
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

// ===== HELPER FUNCTIONS =====

/**
 * Format timestamp to readable date.
 */
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return format.format(date)
}

//private fun com.example.neokratos.data.local.entity.SetLogEntity.estimateOneRepMax(): Float {
//    return com.example.neokratos.data.local.entity.estimateOneRepMax(this)
//}
//
//private fun com.example.neokratos.data.local.entity.SetLogEntity.getVolume(): Float {
//    return com.example.neokratos.data.local.entity.getVolume(this)
//}