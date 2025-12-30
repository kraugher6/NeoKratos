package com.example.neokratos.ui.screen.workouttemplate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WorkoutTemplateScreen(
    viewModel: WorkoutTemplateViewModel
) {
    val templates by viewModel.templates.collectAsState()

    var newName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("New Workout Template") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(onClick = {
            if (newName.isNotBlank()) {
                viewModel.addTemplate(newName)
                newName = ""
            }
        }) {
            Text("Add Template")
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(templates) { template ->
                Text("- ${template.name}")
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
