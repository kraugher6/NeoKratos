package com.example.neokratos.ui.screen.activeworkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neokratos.data.local.entity.getVolume
import com.example.neokratos.data.local.relations.SessionExerciseWithDetails
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

/**
 * Card showing an exercise with all its sets.
 *
 * Features:
 * - Exercise name and details
 * - List of completed sets
 * - Quick add set button
 * - Previous workout comparison
 */
@Composable
fun ExerciseCard(
    exerciseWithDetails: SessionExerciseWithDetails,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAddSet: (weight: Float, reps: Int, rpe: Float?) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSetDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    val exercise = exerciseWithDetails.exercise
    val sets = exerciseWithDetails.sets

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: exercise name + actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exercise.category.name} • ${exercise.equipment.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Remove exercise button
                IconButton(onClick = { showRemoveDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove exercise",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sets list
            if (sets.isEmpty()) {
                Text(
                    text = "No sets yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Set headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Set",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        text = "Weight",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(70.dp)
                    )
                    Text(
                        text = "Reps",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(50.dp)
                    )
                    Text(
                        text = "RPE",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(50.dp)
                    )
                    Text(
                        text = "Volume",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(70.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Set rows
                sets.forEach { set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${set.setNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(40.dp)
                        )
                        Text(
                            text = "${set.weight} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(70.dp)
                        )
                        Text(
                            text = "${set.reps}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(50.dp)
                        )
                        Text(
                            text = set.rpe?.let { "%.1f".format(it) } ?: "-",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(50.dp)
                        )
                        Text(
                            text = "${set.getVolume().toInt()} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(70.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total: ${sets.size} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Volume: ${sets.sumOf { it.getVolume().toDouble() }.toInt()} kg",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add set button
            Button(
                onClick = { showAddSetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Set")
            }
        }
    }

    // Add set dialog
    if (showAddSetDialog) {
        AddSetDialog(
            previousSet = sets.lastOrNull(),
            onConfirm = { weight, reps, rpe ->
                onAddSet(weight, reps, rpe)
                showAddSetDialog = false
            },
            onDismiss = { showAddSetDialog = false }
        )
    }

    // Remove exercise dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Exercise?") },
            text = { Text("This will delete all sets for ${exercise.name}") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove()
                    showRemoveDialog = false
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Dialog for adding a new set.
 * Pre-fills with previous set data.
 */
@Composable
private fun AddSetDialog(
    previousSet: com.example.neokratos.data.local.entity.SetLogEntity?,
    onConfirm: (weight: Float, reps: Int, rpe: Float?) -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableStateOf(previousSet?.weight?.toString() ?: "") }
    var reps by remember { mutableStateOf(previousSet?.reps?.toString() ?: "") }
    var rpe by remember { mutableStateOf(previousSet?.rpe?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Set") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Weight input
                OutlinedTextField(
                    value = weight,
                    onValueChange = { newValue ->
                        // Only allow numbers and single decimal point
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            weight = newValue
                        }
                    },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Reps input
                OutlinedTextField(
                    value = reps,
                    onValueChange = { newValue ->
                        // Only allow integer numbers
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                            reps = newValue
                        }
                    },
                    label = { Text("Reps") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // RPE input
                OutlinedTextField(
                    value = rpe,
                    onValueChange = { newValue ->
                        // Only allow numbers with single decimal, max 10
                        if (newValue.isEmpty() || newValue.matches(Regex("^([0-9]|10)(\\.\\d?)?$"))) {
                            rpe = newValue
                        }
                    },
                    label = { Text("RPE (optional)") },
                    placeholder = { Text("1-10") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (previousSet != null) {
                    Text(
                        text = "Previous: ${previousSet.weight}kg × ${previousSet.reps} @ RPE ${previousSet.rpe ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val w = weight.toFloatOrNull() ?: return@TextButton
                    val r = reps.toIntOrNull() ?: return@TextButton
                    val rpeValue = rpe.toFloatOrNull()

                    onConfirm(w, r, rpeValue)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}