package com.example.neokratos.ui.screen.workout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onNavigateHistory: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(onClick = viewModel::startWorkout) {
            Text("Start Workout")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateHistory) {
            Text("Workout History")
        }
    }
}
