package com.example.neokratos.ui.screen.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neokratos.data.local.entity.getDateDisplay
import com.example.neokratos.data.local.entity.getDisplayName
import com.example.neokratos.data.local.entity.getDurationDisplay
import com.example.neokratos.data.local.entity.getTimeDisplay
import com.example.neokratos.data.local.entity.getVolume
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.local.relations.getAverageRPE
import com.example.neokratos.data.local.relations.getTotalExercises
import com.example.neokratos.data.local.relations.getTotalSets
import com.example.neokratos.data.local.relations.getTotalVolume

/**
 * Screen showing detailed view of a completed workout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    sessionId: Long,
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val workout by viewModel.getWorkoutDetail(sessionId).collectAsStateWithLifecycle(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.session?.getDisplayName() ?: "Workout Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (workout == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            WorkoutDetailContent(
                workout = workout!!,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@Composable
private fun WorkoutDetailContent(
    workout: SessionComplete,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Workout summary card
        item {
            WorkoutSummaryCard(workout)
        }

        // Exercises
        item {
            Text(
                text = "Exercises",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(workout.exercisesWithDetails) { exerciseWithDetails ->
            ExerciseDetailCard(exerciseWithDetails)
        }
    }
}

@Composable
private fun WorkoutSummaryCard(workout: SessionComplete) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem("Date", workout.session.getDateDisplay())
                SummaryItem("Time", workout.session.getTimeDisplay())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem("Duration", workout.session.getDurationDisplay())
                SummaryItem("Volume", "${workout.getTotalVolume().toInt()} kg")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem("Exercises", workout.getTotalExercises().toString())
                SummaryItem("Sets", workout.getTotalSets().toString())
            }

            workout.getAverageRPE()?.let { avgRPE ->
                SummaryItem("Avg RPE", "%.1f".format(avgRPE))
            }

            workout.session.notes?.let { notes ->
                Divider()
                Column {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExerciseDetailCard(
    exerciseWithDetails: com.example.neokratos.data.local.relations.SessionExerciseWithDetails
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Exercise name
            Text(
                text = exerciseWithDetails.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${exerciseWithDetails.exercise.category.name} • ${exerciseWithDetails.exercise.equipment.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider()

            // Sets table header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
                Text("Weight", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(70.dp))
                Text("Reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(50.dp))
                Text("RPE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(50.dp))
                Text("Volume", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(70.dp))
            }

            // Sets
            exerciseWithDetails.sets.forEach { set ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${set.setNumber}", modifier = Modifier.width(40.dp))
                    Text("${set.weight} kg", modifier = Modifier.width(70.dp))
                    Text("${set.reps}", modifier = Modifier.width(50.dp))
                    Text(set.rpe?.let { "%.1f".format(it) } ?: "-", modifier = Modifier.width(50.dp))
                    Text(
                        "${set.getVolume().toInt()} kg",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(70.dp)
                    )
                }
            }

            Divider()

            // Exercise summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${exerciseWithDetails.sets.size} sets",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Total: ${exerciseWithDetails.getTotalVolume().toInt()} kg",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}