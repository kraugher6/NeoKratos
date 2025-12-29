package com.example.neokratos.ui.screen.workout

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun WorkoutScreen(viewModel: WorkoutViewModel) {

    var exercise by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(
            onClick = { viewModel.startWorkout() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Workout")
        }

        OutlinedTextField(
            value = exercise,
            onValueChange = { exercise = it },
            label = { Text("Exercise") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = reps,
            onValueChange = { reps = it },
            label = { Text("Reps") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight (kg)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                viewModel.addSet(
                    exercise = exercise,
                    reps = reps.toIntOrNull() ?: 0,
                    weight = weight.toFloatOrNull() ?: 0f
                )
                Toast.makeText(context, "Set added", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.currentWorkoutId != null
        ) {
            Text("Add Set")
        }
    }
}
