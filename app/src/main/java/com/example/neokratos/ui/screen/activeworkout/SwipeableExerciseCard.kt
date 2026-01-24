package com.example.neokratos.ui.screen.activeworkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.entity.getVolume
import com.example.neokratos.data.local.relations.SessionExerciseWithDetails

/**
 * Swipeable exercise card - FULL SCREEN card for each exercise.
 *
 * Layout:
 * - Exercise header with name, category, equipment
 * - Position indicator (e.g., "3/5")
 * - Sets table
 * - Add set button at bottom
 * - Menu for delete/edit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableExerciseCard(
    exerciseWithDetails: SessionExerciseWithDetails,
    exerciseNumber: Int,
    totalExercises: Int,
    onAddSet: (weight: Float, reps: Int, rpe: Float?, restSeconds: Int) -> Unit,
    onUpdateSet: (SetLogEntity) -> Unit,
    onRemoveExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSetDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var setToEdit by remember { mutableStateOf<SetLogEntity?>(null) }

    val exercise = exerciseWithDetails.exercise
    val sets = exerciseWithDetails.sets

    // Default rest time from first set or 90s
    val defaultRestSeconds = sets.firstOrNull()?.restSeconds ?: 90

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Exercise number
                    Text(
                        text = "Exercise $exerciseNumber of $totalExercises",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Exercise name - BIG
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Category + Equipment
                    Text(
                        text = "${exercise.category.name} • ${exercise.equipment.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Menu button
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Remove Exercise") },
                            onClick = {
                                showMenu = false
                                showRemoveDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Sets section
            if (sets.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No sets yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the button below to log your first set",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Sets table
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Table header
                    SetTableHeader()

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sets list (scrollable)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = sets,
                            key = { it.id }
                        ) { set ->
                            SetRow(
                                set = set,
                                onClick = { setToEdit = set }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Summary
                    SetsSummary(sets = sets)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add set button - ALWAYS visible at bottom
            Button(
                onClick = { showAddSetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (sets.isEmpty()) "Log First Set" else "Add Set",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Dialogs
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
            onConfirm = { editedSet ->
                // ALWAYS update the set, never add a new one
                onUpdateSet(editedSet)
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
                TextButton(
                    onClick = {
                        onRemoveExercise()
                        showRemoveDialog = false
                    }
                ) {
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
 * Table header for sets.
 */
@Composable
private fun SetTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Set",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = "Weight",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = "Reps",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = "RPE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = "Volume",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(70.dp)
        )
    }
}

/**
 * Single set row - clickable to edit.
 */
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
            .background(backgroundColor, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${set.setNumber}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (set.completed) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = if (set.weight > 0) "${set.weight} kg" else "-",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = if (set.reps > 0) "${set.reps}" else "-",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = set.rpe?.let { "%.1f".format(it) } ?: "-",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(50.dp)
        )
        Text(
            text = if (set.completed && set.weight > 0) "${set.getVolume().toInt()} kg" else "-",
            style = MaterialTheme.typography.bodyLarge,
            color = if (set.completed) MaterialTheme.colorScheme.primary else Color.Unspecified,
            fontWeight = if (set.completed) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(70.dp)
        )
    }
}

/**
 * Summary stats for sets.
 */
@Composable
private fun SetsSummary(sets: List<SetLogEntity>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total Sets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${sets.count { it.completed }} / ${sets.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Volume",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${sets.filter { it.completed }.sumOf { it.getVolume().toDouble() }.toInt()} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Add set dialog.
 */
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

/**
 * Edit set dialog.
 */
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
                                completed = true
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