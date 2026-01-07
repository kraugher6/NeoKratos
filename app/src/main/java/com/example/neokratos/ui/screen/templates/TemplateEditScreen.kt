package com.example.neokratos.ui.screen.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neokratos.data.local.entity.TemplateExerciseEntity
import com.example.neokratos.data.local.entity.getSetsRepsDisplay

/**
 * Screen for editing a template (adding/removing exercises, setting targets).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    templateId: Long,
    viewModel: TemplateEditViewModel,
    onBack: () -> Unit
) {
    val template by viewModel.getTemplateWithExercises(templateId).collectAsStateWithLifecycle(null)
    var showExercisePicker by remember { mutableStateOf(false) }

    LaunchedEffect(templateId) {
        viewModel.loadTemplate(templateId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(template?.template?.name ?: "Edit Template") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showExercisePicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
            }
        }
    ) { padding ->
        if (template == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            TemplateEditContent(
                template = template!!,
                onRemoveExercise = { exerciseId ->
                    viewModel.removeExerciseFromTemplate(exerciseId)
                },
                onUpdateExercise = { exercise ->
                    viewModel.updateTemplateExercise(exercise)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }

    // Exercise picker dialog
    if (showExercisePicker) {
        com.example.neokratos.ui.screen.activeworkout.ExercisePickerDialogScreen(
            onExerciseSelected = { exerciseId ->
                viewModel.addExerciseToTemplate(templateId, exerciseId)
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false }
        )
    }
}

@Composable
private fun TemplateEditContent(
    template: com.example.neokratos.data.local.relations.TemplateWithExerciseDetails,
    onRemoveExercise: (Long) -> Unit,
    onUpdateExercise: (TemplateExerciseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (template.exercisesWithDetails.isEmpty()) {
        EmptyTemplateState(
            modifier = modifier.padding(16.dp)
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = template.exercisesWithDetails,
                key = { it.templateExercise.id }
            ) { exerciseWithDetails ->
                TemplateExerciseCard(
                    exerciseWithDetails = exerciseWithDetails,
                    onRemove = { onRemoveExercise(exerciseWithDetails.templateExercise.id) },
                    onUpdate = onUpdateExercise
                )
            }
        }
    }
}

@Composable
private fun EmptyTemplateState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Exercises Yet",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to add exercises to this template",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TemplateExerciseCard(
    exerciseWithDetails: com.example.neokratos.data.local.relations.TemplateExerciseWithDetails,
    onRemove: () -> Unit,
    onUpdate: (TemplateExerciseEntity) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showEditDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exerciseWithDetails.templateExercise.getSetsRepsDisplay(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove exercise",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Edit dialog
    if (showEditDialog) {
        EditExerciseDialog(
            exercise = exerciseWithDetails.templateExercise,
            exerciseName = exerciseWithDetails.exercise.name,
            onConfirm = { updated ->
                onUpdate(updated)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Exercise?") },
            text = { Text("Remove ${exerciseWithDetails.exercise.name} from this template?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Dialog for editing exercise targets (sets, reps).
 */
@Composable
private fun EditExerciseDialog(
    exercise: TemplateExerciseEntity,
    exerciseName: String,
    onConfirm: (TemplateExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var sets by remember { mutableStateOf(exercise.targetSets.toString()) }
    var repsMin by remember { mutableStateOf(exercise.targetRepsMin.toString()) }
    var repsMax by remember { mutableStateOf(exercise.targetRepsMax.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $exerciseName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Sets input
                OutlinedTextField(
                    value = sets,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                            sets = newValue
                        }
                    },
                    label = { Text("Target Sets") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reps min
                    OutlinedTextField(
                        value = repsMin,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                                repsMin = newValue
                            }
                        },
                        label = { Text("Min Reps") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Reps max
                    OutlinedTextField(
                        value = repsMax,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                                repsMax = newValue
                            }
                        },
                        label = { Text("Max Reps") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Example: 3 sets of 8-12 reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val s = sets.toIntOrNull() ?: return@TextButton
                    val rMin = repsMin.toIntOrNull() ?: return@TextButton
                    val rMax = repsMax.toIntOrNull() ?: return@TextButton

                    onConfirm(
                        exercise.copy(
                            targetSets = s,
                            targetRepsMin = rMin,
                            targetRepsMax = rMax
                        )
                    )
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