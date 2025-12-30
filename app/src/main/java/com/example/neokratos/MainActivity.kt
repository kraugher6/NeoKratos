package com.example.neokratos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neokratos.data.local.GymDatabase
import com.example.neokratos.data.repository.WorkoutRepository
import com.example.neokratos.data.repository.WorkoutTemplateRepository
import com.example.neokratos.ui.navigation.NavRoutes
import com.example.neokratos.ui.screen.history.*
import com.example.neokratos.ui.screen.workout.*
import com.example.neokratos.ui.screen.workouttemplate.WorkoutTemplateScreen
import com.example.neokratos.ui.screen.workouttemplate.WorkoutTemplateViewModel
import com.example.neokratos.ui.screen.workouttemplate.WorkoutTemplateViewModelFactory
import com.example.neokratos.ui.theme.NeoKratosTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = GymDatabase.getInstance(this)
        val workoutRepository = WorkoutRepository(database.workoutDao())
        val workoutTemplateRepository = WorkoutTemplateRepository(database.workoutTemplateDao())

        setContent {
            NeoKratosTheme(dynamicColor = false) {

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.Workout.route
                ) {

                    composable(NavRoutes.Workout.route) {
                        val vm: WorkoutViewModel = viewModel(
                            factory = WorkoutViewModelFactory(workoutRepository)
                        )

                        WorkoutScreen(
                            viewModel = vm,
                            onNavigateHistory = { navController.navigate(NavRoutes.History.route) }
                        )
                    }

                    composable(NavRoutes.History.route) {
                        val vm: HistoryViewModel = viewModel(
                            factory = HistoryViewModelFactory(workoutRepository)
                        )

                        HistoryScreen(
                            viewModel = vm,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
