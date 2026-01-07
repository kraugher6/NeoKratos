package com.example.neokratos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neokratos.data.local.GymDatabase
import com.example.neokratos.data.repository.ExerciseRepository
import com.example.neokratos.data.repository.WorkoutSessionRepository
import com.example.neokratos.data.repository.WorkoutTemplateRepository
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutScreen
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutViewModel
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutViewModelFactory
import com.example.neokratos.ui.screen.exercises.ExerciseLibraryScreen
import com.example.neokratos.ui.screen.exercises.ExerciseLibraryViewModel
import com.example.neokratos.ui.screen.exercises.ExerciseLibraryViewModelFactory
import com.example.neokratos.ui.screen.history.HistoryScreen
import com.example.neokratos.ui.screen.history.HistoryViewModel
import com.example.neokratos.ui.screen.history.HistoryViewModelFactory
import com.example.neokratos.ui.screen.history.WorkoutDetailScreen
import com.example.neokratos.ui.screen.home.HomeScreen
import com.example.neokratos.ui.screen.templates.TemplateEditViewModel
import com.example.neokratos.ui.screen.templates.TemplateListScreen
import com.example.neokratos.ui.screen.templates.TemplateViewModel
import com.example.neokratos.ui.theme.NeoKratosTheme
import kotlinx.coroutines.launch

/**
 * Main Activity - Entry point for the app.
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
            setLogDao = database.setLogDao(),
            templateExerciseDao = database.templateExerciseDao() // ADDED for template integration
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
                // Navigation for workout detail and template edit
                val navController = rememberNavController()
                var selectedWorkoutId by remember { mutableStateOf<Long?>(null) }
                var selectedTemplateId by remember { mutableStateOf<Long?>(null) }

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

                val exerciseLibraryViewModel: ExerciseLibraryViewModel = viewModel(
                    factory = ExerciseLibraryViewModelFactory(exerciseRepository)
                )

                val templateEditViewModel: TemplateEditViewModel = viewModel(
                    factory = com.example.neokratos.ui.screen.templates.TemplateEditViewModelFactory(
                        templateDao = database.workoutTemplateDao(),
                        templateExerciseDao = database.templateExerciseDao()
                    )
                )

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            workoutScreen = {
                                ActiveWorkoutScreen(
                                    viewModel = activeWorkoutViewModel,
                                    onNavigateBack = { }
                                )
                            },
                            templatesScreen = {
                                TemplateListScreen(
                                    viewModel = templateViewModel,
                                    onStartWorkout = { templateId ->
                                        activeWorkoutViewModel.startWorkout(templateId = templateId)
                                    },
                                    onEditTemplate = { templateId ->
                                        selectedTemplateId = templateId
                                        navController.navigate("template_edit")
                                    }
                                )
                            },
                            historyScreen = {
                                HistoryScreen(
                                    viewModel = historyViewModel,
                                    onWorkoutClick = { workoutId ->
                                        selectedWorkoutId = workoutId
                                        navController.navigate("workout_detail")
                                    }
                                )
                            },
                            exercisesScreen = {
                                ExerciseLibraryScreen(viewModel = exerciseLibraryViewModel)
                            }
                        )
                    }

                    composable("workout_detail") {
                        selectedWorkoutId?.let { workoutId ->
                            WorkoutDetailScreen(
                                sessionId = workoutId,
                                viewModel = historyViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    composable("template_edit") {
                        selectedTemplateId?.let { templateId ->
                            com.example.neokratos.ui.screen.templates.TemplateEditScreen(
                                templateId = templateId,
                                viewModel = templateEditViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}