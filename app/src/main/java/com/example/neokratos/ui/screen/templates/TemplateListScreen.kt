package com.example.neokratos.ui.screen.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neokratos.data.local.entity.getSetsRepsDisplay
import com.example.neokratos.data.local.relations.TemplateWithExerciseDetails
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    viewModel: TemplateViewModel,
    onStartWorkout: (templateId: Long) -> Unit = {},
    onEditTemplate: (templateId: Long) -> Unit = {}
) {
    val templates by viewModel.templatesWithDetails.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    // FIX: Create coroutine scope for Composable
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create template")
            }
        }
    ) { padding ->
        if (templates.isEmpty()) {
            EmptyTemplatesState(
                onCreateTemplate = { showCreateDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = templates,
                    key = { it.template.id }
                ) { templateWithDetails ->
                    TemplateCard(
                        templateWithDetails = templateWithDetails,
                        onStartWorkout = { onStartWorkout(templateWithDetails.template.id) },
                        onEdit = { onEditTemplate(templateWithDetails.template.id) },
                        onDelete = { viewModel.deleteTemplate(templateWithDetails.template.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTemplateDialog(
            onConfirm = { name ->
                // FIX 1: Create template and navigate to edit
                // Use the scope defined at the top of the Composable
                scope.launch {
                    val templateId = viewModel.createTemplateAndGetId(name)
                    showCreateDialog = false
                    onEditTemplate(templateId)
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun EmptyTemplatesState(
    onCreateTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Templates Yet",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create a template to save your workout routine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateTemplate) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Template")
        }
    }
}

@Composable
private fun TemplateCard(
    templateWithDetails: TemplateWithExerciseDetails,
    onStartWorkout: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = templateWithDetails.template.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete template",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = "${templateWithDetails.exercisesWithDetails.size} exercises",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (templateWithDetails.exercisesWithDetails.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    templateWithDetails.exercisesWithDetails.take(3).forEach { exerciseWithDetails ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = exerciseWithDetails.exercise.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = exerciseWithDetails.templateExercise.getSetsRepsDisplay(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (templateWithDetails.exercisesWithDetails.size > 3) {
                        Text(
                            text = "+${templateWithDetails.exercisesWithDetails.size - 3} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Workout")
                }

                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Template?") },
            text = { Text("This will permanently delete \"${templateWithDetails.template.name}\" and all its exercises.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
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
private fun CreateTemplateDialog(
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var templateName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Template") },
        text = {
            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text("Template Name") },
                placeholder = { Text("e.g., Push Day, Leg Day") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (templateName.isNotBlank()) {
                        onConfirm(templateName.trim())
                    }
                },
                enabled = templateName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}