package com.example.neokratos.ui.screen.activeworkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.relations.SessionExerciseWithDetails
import com.example.neokratos.data.local.entity.getVolume

@Composable
fun ExerciseCard(
    exerciseWithDetails: SessionExerciseWithDetails,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAddSet: (weight: Float, reps: Int, rpe: Float?, restSeconds: Int) -> Unit,
    onRemove: () -> Unit,
    onUpdateSet: (SetLogEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddSetDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var setToEdit by remember { mutableStateOf<SetLogEntity?>(null) }

    val exercise = exerciseWithDetails.exercise
    val sets = exerciseWithDetails.sets

    // Get default rest time from first set or use 90 seconds
    val defaultRestSeconds = sets.firstOrNull()?.restSeconds ?: 90

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

                IconButton(onClick = { showRemoveDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove exercise",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sets.isEmpty()) {
                Text(
                    text = "No sets yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Set",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(30.dp)
                    )
                    Text(
                        text = "Weight",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        text = "Reps",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(45.dp)
                    )
                    Text(
                        text = "RPE",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(45.dp)
                    )
                    Text(
                        text = "Volume",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(65.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                sets.forEach { set ->
                    SetRow(
                        set = set,
                        onClick = { setToEdit = set }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total: ${sets.count { it.completed }} / ${sets.size} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Volume: ${sets.filter { it.completed }.sumOf { it.getVolume().toDouble() }.toInt()} kg",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
        val currentSet = setToEdit!!
        EditSetDialog(
            set = currentSet,
            onConfirm = { editedSet ->  // ← Riceve SetLogEntity completo
                val wasPlaceholder = !currentSet.completed

                if (wasPlaceholder) {
                    // Placeholder being completed: extract values and use "Add Set" flow
                    onAddSet(
                        editedSet.weight,
                        editedSet.reps,
                        editedSet.rpe,
                        currentSet.restSeconds ?: defaultRestSeconds
                    )
                } else {
                    // Already completed set: pass the updated entity
                    onUpdateSet(editedSet)
                }

                setToEdit = null
            },
            onDismiss = { setToEdit = null }
        )
    }

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

@Composable
private fun SetRow(
    set: SetLogEntity,
    onClick: () -> Unit
) {
    val backgroundColor = if (set.completed) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(backgroundColor, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${set.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (set.completed) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(30.dp)
        )
        Text(
            text = if (set.weight > 0) "${set.weight} kg" else "-",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = if (set.reps > 0) "${set.reps}" else "-",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(45.dp)
        )
        Text(
            text = set.rpe?.let { "%.1f".format(it) } ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(45.dp)
        )
        Text(
            text = if (set.completed && set.weight > 0) "${set.getVolume().toInt()} kg" else "-",
            style = MaterialTheme.typography.bodyMedium,
            color = if (set.completed) MaterialTheme.colorScheme.primary else Color.Unspecified,
            fontWeight = if (set.completed) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(65.dp)
        )
    }
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { newValue ->
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

                OutlinedTextField(
                    value = reps,
                    onValueChange = { newValue ->
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

                OutlinedTextField(
                    value = rpe,
                    onValueChange = { newValue ->
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

                // Show rest timer info
                set.restSeconds?.let { rest ->
                    val minutes = rest / 60
                    val seconds = rest % 60
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "Rest timer: ${minutes}:${seconds.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
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
                                completed = true // Mark as completed
                            )
                        )
                    }
                }
            ) {
                Text("Save")
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
private fun AddSetDialog(
    previousSet: SetLogEntity?,
    defaultRestSeconds: Int,
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
                OutlinedTextField(
                    value = weight,
                    onValueChange = { newValue ->
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

                OutlinedTextField(
                    value = reps,
                    onValueChange = { newValue ->
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

                OutlinedTextField(
                    value = rpe,
                    onValueChange = { newValue ->
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

                // Previous set info
                if (previousSet != null) {
                    HorizontalDivider()
                    Text(
                        text = "Previous set",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${previousSet.weight}kg × ${previousSet.reps} @ RPE ${previousSet.rpe ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Rest timer info
                HorizontalDivider()
                val minutes = defaultRestSeconds / 60
                val seconds = defaultRestSeconds % 60
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Rest timer will start",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${minutes}:${seconds.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
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
                Text("Save & Start Timer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}