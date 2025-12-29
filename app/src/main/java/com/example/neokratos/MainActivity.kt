package com.example.neokratos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neokratos.data.local.GymDatabase
import com.example.neokratos.data.repository.WorkoutRepository
import com.example.neokratos.ui.screen.workout.WorkoutScreen
import com.example.neokratos.ui.screen.workout.WorkoutViewModel
import com.example.neokratos.ui.screen.workout.WorkoutViewModelFactory
import com.example.neokratos.ui.theme.NeoKratosTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = GymDatabase.getInstance(this)
        val repository = WorkoutRepository(database.workoutDao())
        val viewModelFactory = WorkoutViewModelFactory(repository)

        setContent {
            NeoKratosTheme {
                val viewModel: WorkoutViewModel = viewModel(
                    factory = viewModelFactory
                )

                WorkoutScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NeoKratosTheme {
        Greeting("Android")
    }
}