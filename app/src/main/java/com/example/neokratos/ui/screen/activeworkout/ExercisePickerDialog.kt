package com.example.neokratos.ui.screen.activeworkout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neokratos.data.local.ExerciseCategory
import com.example.neokratos.data.local.entity.ExerciseEntity

/**
 * Full-screen dialog for selecting an exercise from the library.
 *
 * Standalone composable that manages its own state.
 * Gets exercises from database directly via ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerDialogScreen(
    onExerciseSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Local state for filtering
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExerciseCategory?>(null) }

    // Get exercises from database (simplified - will get from repository later)
    // For now, use dummy data
    val allExercises = remember { getDummyExercises() }

    // Filter exercises
    val filteredExercises = remember(searchQuery, selectedCategory, allExercises) {
        allExercises.filter { exercise ->
            val matchesCategory = selectedCategory == null || exercise.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() ||
                    exercise.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Exercise") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                // Category filter chips
                CategoryFilterRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Exercise list
                if (filteredExercises.isEmpty()) {
                    EmptyExerciseListState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredExercises,
                            key = { it.id }
                        ) { exercise ->
                            ExerciseListItem(
                                exercise = exercise,
                                onClick = { onExerciseSelected(exercise.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dummy exercises for testing.
 * In real app, this would come from ExerciseRepository via ViewModel.
 */
private fun getDummyExercises(): List<ExerciseEntity> {
    return listOf(
        ExerciseEntity(
            id = 1,
            name = "Bench Press",
            category = ExerciseCategory.CHEST,
            primaryMuscleGroup = com.example.neokratos.data.local.MuscleGroup.PECTORALS_MIDDLE,
            secondaryMuscleGroups = listOf(
                com.example.neokratos.data.local.MuscleGroup.TRICEPS,
                com.example.neokratos.data.local.MuscleGroup.DELTS_ANTERIOR
            ),
            equipment = com.example.neokratos.data.local.Equipment.BARBELL
        ),
        ExerciseEntity(
            id = 2,
            name = "Back Squat",
            category = ExerciseCategory.LEGS,
            primaryMuscleGroup = com.example.neokratos.data.local.MuscleGroup.QUADRICEPS,
            secondaryMuscleGroups = listOf(
                com.example.neokratos.data.local.MuscleGroup.GLUTES,
                com.example.neokratos.data.local.MuscleGroup.HAMSTRINGS
            ),
            equipment = com.example.neokratos.data.local.Equipment.BARBELL
        ),
        ExerciseEntity(
            id = 3,
            name = "Pull-ups",
            category = ExerciseCategory.BACK,
            primaryMuscleGroup = com.example.neokratos.data.local.MuscleGroup.LATS,
            secondaryMuscleGroups = listOf(
                com.example.neokratos.data.local.MuscleGroup.BICEPS
            ),
            equipment = com.example.neokratos.data.local.Equipment.BODYWEIGHT
        )
    )
}

/**
 * Search bar for filtering exercises by name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search exercises...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
}

/**
 * Row of filter chips for exercise categories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    selectedCategory: ExerciseCategory?,
    onCategorySelected: (ExerciseCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }

        // Category chips
        items(ExerciseCategory.entries.toTypedArray()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

/**
 * List item showing exercise details.
 */
@Composable
private fun ExerciseListItem(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Exercise name
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Category and equipment
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category badge
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = exercise.category.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )

                // Equipment badge
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = exercise.equipment.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            // Primary muscle group
            if (exercise.primaryMuscleGroup != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Primary: ${exercise.primaryMuscleGroup.name.lowercase().replace("_", " ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Secondary muscle groups
            if (exercise.secondaryMuscleGroups?.isNotEmpty() == true) {
                Text(
                    text = "Secondary: ${exercise.secondaryMuscleGroups.joinToString(", ") {
                        it.name.lowercase().replace("_", " ")
                    }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Empty state when no exercises found.
 */
@Composable
private fun EmptyExerciseListState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No exercises found",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Try a different search or category",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}