package com.example.neokratos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neokratos.data.local.GymDatabase
import com.example.neokratos.data.repository.ExerciseRepository
import com.example.neokratos.data.repository.WorkoutSessionRepository
import com.example.neokratos.data.repository.WorkoutTemplateRepository
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutScreen
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutViewModel
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutViewModelFactory
import com.example.neokratos.ui.screen.history.HistoryScreen
import com.example.neokratos.ui.screen.history.HistoryViewModel
import com.example.neokratos.ui.screen.history.HistoryViewModelFactory
import com.example.neokratos.ui.screen.home.HomeScreen
import com.example.neokratos.ui.screen.templates.TemplateListScreen
import com.example.neokratos.ui.screen.templates.TemplateManageScreen
import com.example.neokratos.ui.screen.templates.TemplateViewModel
import com.example.neokratos.ui.theme.NeoKratosTheme
import kotlinx.coroutines.launch

/**
 * Main Activity - Entry point for the app.
 *
 * Sets up:
 * - Database
 * - Repositories
 * - ViewModels
 * - Navigation
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Database
        val database = GymDatabase.getInstance(this)

        // Repositories
        val workoutSessionRepository = WorkoutSessionRepository(
            workoutSessionDao = database.workoutSessionDao(),
            sessionExerciseDao = database.sessionExerciseDao(),
            setLogDao = database.setLogDao()
        )

        val templateRepository = WorkoutTemplateRepository(
            database.workoutTemplateDao()
        )

        val exerciseRepository = ExerciseRepository(
            database.exerciseDao()
        )

        // Seed exercises on first launch
        lifecycleScope.launch {
            if (exerciseRepository.needsSeedData()) {
                exerciseRepository.insertSeedExercises()
            }
        }

        setContent {
            NeoKratosTheme(dynamicColor = false) {

                // ViewModels
                val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
                    factory = ActiveWorkoutViewModelFactory(
                        workoutSessionRepository = workoutSessionRepository,
                        exerciseRepository = exerciseRepository
                    )
                )

                val templateViewModel: TemplateViewModel = viewModel {
                    TemplateViewModel(database.workoutTemplateDao())
                }

                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(workoutSessionRepository)
                )

                // Main navigation
                HomeScreen(
                    workoutScreen = {
                        ActiveWorkoutScreen(
                            viewModel = activeWorkoutViewModel,
                            onNavigateBack = { /* Handle navigation if needed */ }
                        )
                    },
                    templatesScreen = {
                        TemplateListScreen(viewModel = templateViewModel)
                    },
                    manageTemplatesScreen = {
                        TemplateManageScreen(viewModel = templateViewModel)
                    },
                    historyScreen = {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onBack = { /* Not needed for now */ }
                        )
                    }
                )
            }
        }
    }
}