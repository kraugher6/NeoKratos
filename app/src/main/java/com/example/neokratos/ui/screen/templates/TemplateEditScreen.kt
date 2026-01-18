package com.example.neokratos.ui.screen.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neokratos.data.local.entity.TemplateExerciseEntity
import com.example.neokratos.data.local.relations.TemplateWithExerciseDetails

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
                // FIX 4: Replace back arrow with Save button
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save and return",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
    template: TemplateWithExerciseDetails,
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

data class SetConfig(
    val setNumber: Int,
    val repsMin: Int,
    val repsMax: Int,
    val weight: Float? = null,
    val rpe: Float? = null
)

@Composable
private fun TemplateExerciseCard(
    exerciseWithDetails: com.example.neokratos.data.local.relations.TemplateExerciseWithDetails,
    onRemove: () -> Unit,
    onUpdate: (TemplateExerciseEntity) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
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
                        text = exerciseWithDetails.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exerciseWithDetails.exercise.category.name} • ${exerciseWithDetails.exercise.equipment.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit exercise"
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

            Spacer(modifier = Modifier.height(8.dp))

            val te = exerciseWithDetails.templateExercise
            Text(
                text = "${te.targetSets} sets × ${te.targetRepsMin}-${te.targetRepsMax} reps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            te.restSeconds?.let { rest ->
                val minutes = rest / 60
                val seconds = rest % 60
                Text(
                    text = "Rest: ${minutes}:${seconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (te.notes != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = te.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }

    if (showEditDialog) {
        AdvancedEditExerciseDialog(
            exercise = exerciseWithDetails.templateExercise,
            exerciseName = exerciseWithDetails.exercise.name,
            onConfirm = { updated ->
                onUpdate(updated)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

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

@Composable
private fun AdvancedEditExerciseDialog(
    exercise: TemplateExerciseEntity,
    exerciseName: String,
    onConfirm: (TemplateExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(0) }

    var sets by remember { mutableStateOf(exercise.targetSets.toString()) }
    var repsMin by remember { mutableStateOf(exercise.targetRepsMin.toString()) }
    var repsMax by remember { mutableStateOf(exercise.targetRepsMax.toString()) }

    var setConfigs by remember {
        mutableStateOf(
            List(exercise.targetSets) { index ->
                SetConfig(
                    setNumber = index + 1,
                    repsMin = exercise.targetRepsMin,
                    repsMax = exercise.targetRepsMax
                )
            }
        )
    }

    var restMinutes by remember {
        mutableStateOf((exercise.restSeconds ?: 90) / 60)
    }
    var restSeconds by remember {
        mutableStateOf((exercise.restSeconds ?: 90) % 60)
    }
    var notes by remember { mutableStateOf(exercise.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $exerciseName") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    TabRow(
                        selectedTabIndex = selectedMode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedMode == 0,
                            onClick = { selectedMode = 0 },
                            text = { Text("Simple") }
                        )
                        Tab(
                            selected = selectedMode == 1,
                            onClick = { selectedMode = 1 },
                            text = { Text("Advanced") }
                        )
                    }
                }

                if (selectedMode == 0) {
                    item {
                        OutlinedTextField(
                            value = sets,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                                    sets = newValue
                                    val newCount = newValue.toIntOrNull() ?: 0
                                    if (newCount > 0) {
                                        setConfigs = List(newCount) { index ->
                                            setConfigs.getOrNull(index) ?: SetConfig(
                                                setNumber = index + 1,
                                                repsMin = repsMin.toIntOrNull() ?: 8,
                                                repsMax = repsMax.toIntOrNull() ?: 12
                                            )
                                        }
                                    }
                                }
                            },
                            label = { Text("Target Sets") },
                            placeholder = { Text("3") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = repsMin,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                                        repsMin = newValue
                                    }
                                },
                                label = { Text("Min Reps") },
                                placeholder = { Text("8") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = repsMax,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                                        repsMax = newValue
                                    }
                                },
                                label = { Text("Max Reps") },
                                placeholder = { Text("12") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Configure Each Set",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Number of sets: ${setConfigs.size}")
                            Row {
                                IconButton(
                                    onClick = {
                                        if (setConfigs.size > 1) {
                                            setConfigs = setConfigs.dropLast(1)
                                        }
                                    },
                                    enabled = setConfigs.size > 1
                                ) {
                                    Icon(Icons.Default.Delete, "Remove set")
                                }
                                IconButton(
                                    onClick = {
                                        setConfigs = setConfigs + SetConfig(
                                            setNumber = setConfigs.size + 1,
                                            repsMin = setConfigs.lastOrNull()?.repsMin ?: 8,
                                            repsMax = setConfigs.lastOrNull()?.repsMax ?: 12
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Add, "Add set")
                                }
                            }
                        }
                    }

                    itemsIndexed(setConfigs) { index, setConfig ->
                        SetConfigCard(
                            setConfig = setConfig,
                            onUpdate = { updated ->
                                setConfigs = setConfigs.toMutableList().apply {
                                    this[index] = updated
                                }
                            }
                        )
                    }
                }

                item {
                    HorizontalDivider()
                    Text(
                        text = "Rest Time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Minutes",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = restMinutes.toFloat(),
                                onValueChange = { restMinutes = it.toInt() },
                                valueRange = 0f..5f,
                                steps = 4
                            )
                            Text(
                                text = "$restMinutes min",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Seconds",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = restSeconds.toFloat(),
                                onValueChange = { restSeconds = it.toInt() },
                                valueRange = 0f..55f,
                                steps = 10
                            )
                            Text(
                                text = "$restSeconds sec",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Total Rest: ${restMinutes}:${restSeconds.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("e.g., Piramidale, dropset, pause reps") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedMode == 0) {
                        val s = sets.toIntOrNull() ?: return@TextButton
                        val rMin = repsMin.toIntOrNull() ?: return@TextButton
                        val rMax = repsMax.toIntOrNull() ?: return@TextButton
                        val totalRestSeconds = (restMinutes * 60) + restSeconds

                        onConfirm(
                            exercise.copy(
                                targetSets = s,
                                targetRepsMin = rMin,
                                targetRepsMax = rMax,
                                restSeconds = totalRestSeconds,
                                notes = notes.ifBlank { null }
                            )
                        )
                    } else {
                        val totalRestSeconds = (restMinutes * 60) + restSeconds
                        val firstSet = setConfigs.firstOrNull() ?: return@TextButton

                        onConfirm(
                            exercise.copy(
                                targetSets = setConfigs.size,
                                targetRepsMin = firstSet.repsMin,
                                targetRepsMax = firstSet.repsMax,
                                restSeconds = totalRestSeconds,
                                notes = notes.ifBlank { null }
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
private fun SetConfigCard(
    setConfig: SetConfig,
    onUpdate: (SetConfig) -> Unit
) {
    var repsMin by remember { mutableStateOf(setConfig.repsMin.toString()) }
    var repsMax by remember { mutableStateOf(setConfig.repsMax.toString()) }
    var weight by remember { mutableStateOf(setConfig.weight?.toString() ?: "") }
    var rpe by remember { mutableStateOf(setConfig.rpe?.toString() ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Set ${setConfig.setNumber}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = repsMin,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                            repsMin = newValue
                            onUpdate(setConfig.copy(repsMin = newValue.toIntOrNull() ?: 8))
                        }
                    },
                    label = { Text("Min") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = repsMax,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                            repsMax = newValue
                            onUpdate(setConfig.copy(repsMax = newValue.toIntOrNull() ?: 12))
                        }
                    },
                    label = { Text("Max") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            weight = newValue
                            onUpdate(setConfig.copy(weight = newValue.toFloatOrNull()))
                        }
                    },
                    label = { Text("Weight") },
                    placeholder = { Text("kg") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = rpe,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^([0-9]|10)(\\.\\d?)?$"))) {
                            rpe = newValue
                            onUpdate(setConfig.copy(rpe = newValue.toFloatOrNull()))
                        }
                    },
                    label = { Text("RPE") },
                    placeholder = { Text("1-10") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}