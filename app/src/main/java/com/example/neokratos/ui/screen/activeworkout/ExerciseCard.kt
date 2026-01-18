package com.example.neokratos.ui.screen.activeworkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.relations.SessionExerciseWithDetails
import com.example.neokratos.data.local.entity.getVolume
import java.util.Locale

/**
 * Exercise Card - GYM BRO EDITION
 *
 * Huge numbers you can see from across the gym.
 * No tables, no tiny text. Just pure STRENGTH data.
 */
@Composable
fun ExerciseCard(
    exerciseWithDetails: SessionExerciseWithDetails,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAddSet: (weight: Float, reps: Int, rpe: Float?, restSeconds: Int) -> Unit,
    onRemove: () -> Unit,
    onUpdateSet: (SetLogEntity) -> Unit = {},
    onStartTimer: (restSeconds: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddSetDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var setToEdit by remember { mutableStateOf<SetLogEntity?>(null) }

    val exercise = exerciseWithDetails.exercise
    val sets = exerciseWithDetails.sets
    val defaultRestSeconds = sets.firstOrNull()?.restSeconds ?: 90

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        border = if (isSelected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header - Exercise name + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showRemoveDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sets.isEmpty()) {
                // No sets yet - big call to action
                Text(
                    text = "Tap button to log first set",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Sets - HUGE numbers
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    sets.forEach { set ->
                        SetRow(
                            set = set,
                            onClick = { setToEdit = set }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Total volume - BIG and PROUD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "TOTAL: ${sets.filter { it.completed }.sumOf { it.getVolume().toDouble() }.toInt()}kg",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Set Button - BIG and INVITING
            Button(
                onClick = { showAddSetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "LOG SET",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }

    if (showAddSetDialog) {
        AddSetDialog(
            previousSet = sets.lastOrNull { it.completed },
            defaultRestSeconds = defaultRestSeconds,
            onConfirm = { weight, reps, rpe ->
                onAddSet(weight, reps, rpe, defaultRestSeconds)
                showAddSetDialog = false
            },
            onDismiss = { showAddSetDialog = false }
        )
    }

    if (setToEdit != null) {
        EditSetDialog(
            set = setToEdit!!,
            onConfirm = { updatedSet ->
                val wasPlaceholder = !setToEdit!!.completed
                onUpdateSet(updatedSet)
                if (wasPlaceholder && updatedSet.completed) {
                    updatedSet.restSeconds?.let { onStartTimer(it) }
                }
                setToEdit = null
            },
            onDismiss = { setToEdit = null }
        )
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove ${exercise.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    }
                ) {
                    Text("YES", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("NO")
                }
            }
        )
    }
}

@Composable
private fun SetRow(
    set: SetLogEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (set.completed) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set number - BIG circle
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${set.setNumber}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Weight x Reps - HUGE
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${set.weight}kg × ${set.reps}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )

                // RPE if present
                set.rpe?.let { rpe ->
                    Text(
                        text = "RPE ${String.format(Locale.getDefault(), "%.1f", rpe)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Volume
            Text(
                text = "${set.getVolume().toInt()}kg",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AddSetDialog(
    previousSet: SetLogEntity?,
    defaultRestSeconds: Int,
    onConfirm: (weight: Float, reps: Int, rpe: Float?) -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableStateOf(previousSet?.weight?.toString() ?: "") }
    var reps by remember { mutableStateOf(previousSet?.reps?.toString() ?: "") }
    var rpe by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "LOG SET",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Previous set - if exists
                previousSet?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Previous:")
                            Text(
                                "${it.weight}kg × ${it.reps}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Weight and Reps - BIG input fields
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                weight = it
                            }
                        },
                        label = { Text("KG") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = reps,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                reps = it
                            }
                        },
                        label = { Text("REPS") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // RPE slider
                Column {
                    Text(
                        "RPE (optional)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = rpe.toFloatOrNull() ?: 0f,
                        onValueChange = { rpe = if (it > 0) String.format(Locale.getDefault(), "%.1f", it) else "" },
                        valueRange = 0f..10f,
                        steps = 19
                    )
                    Text(
                        rpe.ifEmpty { "Not set" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toFloatOrNull() ?: return@Button
                    val r = reps.toIntOrNull() ?: return@Button
                    val rpeValue = rpe.toFloatOrNull()
                    onConfirm(w, r, rpeValue)
                },
                enabled = weight.toFloatOrNull() != null && reps.toIntOrNull() != null
            ) {
                Text("LOG")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
private fun EditSetDialog(
    set: SetLogEntity,
    onConfirm: (SetLogEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableStateOf(if (set.weight > 0) set.weight.toString() else "") }
    var reps by remember { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    var rpe by remember { mutableStateOf(set.rpe?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Set ${set.setNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                weight = it
                            }
                        },
                        label = { Text("KG") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = reps,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                                reps = it
                            }
                        },
                        label = { Text("REPS") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Column {
                    Text("RPE")
                    Slider(
                        value = rpe.toFloatOrNull() ?: 0f,
                        onValueChange = { rpe = if (it > 0) String.format(Locale.getDefault(), "%.1f", it) else "" },
                        valueRange = 0f..10f,
                        steps = 19
                    )
                    Text(
                        rpe.ifEmpty { "Not set" },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weight.toFloatOrNull() ?: 0f
                    val r = reps.toIntOrNull() ?: 0
                    val rpeValue = rpe.toFloatOrNull()

                    if (w > 0 && r > 0) {
                        onConfirm(
                            set.copy(
                                weight = w,
                                reps = r,
                                rpe = rpeValue,
                                completed = true
                            )
                        )
                    }
                }
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}