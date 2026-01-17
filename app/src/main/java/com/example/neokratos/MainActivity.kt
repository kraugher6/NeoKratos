package com.example.neokratos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neokratos.data.local.GymDatabase
import com.example.neokratos.data.repository.AnalyticsRepository
import com.example.neokratos.data.repository.BodyMetricsRepository
import com.example.neokratos.data.repository.ExerciseRepository
import com.example.neokratos.data.repository.WorkoutSessionRepository
import com.example.neokratos.data.repository.WorkoutTemplateRepository
import com.example.neokratos.ui.navigation.BottomNavItem
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutScreen
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutViewModel
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutViewModelFactory
import com.example.neokratos.ui.screen.analytics.AnalyticsScreen
import com.example.neokratos.ui.screen.analytics.AnalyticsViewModel
import com.example.neokratos.ui.screen.analytics.AnalyticsViewModelFactory
import com.example.neokratos.ui.screen.analytics.ExerciseAnalyticsScreen
import com.example.neokratos.ui.screen.analytics.ExerciseAnalyticsViewModel
import com.example.neokratos.ui.screen.bodymetrics.BodyMetricsScreen
import com.example.neokratos.ui.screen.bodymetrics.BodyMetricsViewModel
import com.example.neokratos.ui.screen.bodymetrics.BodyMetricsViewModelFactory
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

        val database = GymDatabase.getInstance(this)

        val workoutSessionRepository = WorkoutSessionRepository(
            workoutSessionDao = database.workoutSessionDao(),
            sessionExerciseDao = database.sessionExerciseDao(),
            setLogDao = database.setLogDao(),
            templateExerciseDao = database.templateExerciseDao()
        )

        val templateRepository = WorkoutTemplateRepository(
            database.workoutTemplateDao()
        )

        val exerciseRepository = ExerciseRepository(
            database.exerciseDao()
        )

        val analyticsRepository = AnalyticsRepository(
            workoutSessionDao = database.workoutSessionDao(),
            sessionExerciseDao = database.sessionExerciseDao(),
            setLogDao = database.setLogDao()
        )

        val bodyMetricsRepository = BodyMetricsRepository(
            bodyMetricDao = database.bodyMetricDao()
        )

        lifecycleScope.launch {
            if (exerciseRepository.needsSeedData()) {
                exerciseRepository.insertSeedExercises()
            }
        }

        setContent {
            NeoKratosTheme(dynamicColor = false) {
                // Main navigation controller
                val mainNavController = rememberNavController()

                // State for selected IDs
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

                val analyticsViewModel: AnalyticsViewModel = viewModel(
                    factory = AnalyticsViewModelFactory(analyticsRepository)
                )

                val exerciseAnalyticsViewModel: ExerciseAnalyticsViewModel = viewModel {
                    ExerciseAnalyticsViewModel(analyticsRepository)
                }

                val bodyMetricsViewModel: BodyMetricsViewModel = viewModel(
                    factory = BodyMetricsViewModelFactory(bodyMetricsRepository)
                )

                NavHost(
                    navController = mainNavController,
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
                                        // Start workout and navigate to workout tab
                                        activeWorkoutViewModel.startWorkout(templateId = templateId)
                                    },
                                    onEditTemplate = { templateId ->
                                        selectedTemplateId = templateId
                                        mainNavController.navigate("template_edit")
                                    }
                                )
                            },
                            historyScreen = {
                                HistoryScreen(
                                    viewModel = historyViewModel,
                                    onWorkoutClick = { workoutId ->
                                        selectedWorkoutId = workoutId
                                        mainNavController.navigate("workout_detail")
                                    }
                                )
                            },
                            exercisesScreen = {
                                ExerciseLibraryScreen(viewModel = exerciseLibraryViewModel)
                            },
                            analyticsScreen = {
                                AnalyticsScreen(
                                    viewModel = analyticsViewModel,
                                    onNavigateToExerciseAnalytics = {
                                        mainNavController.navigate("exercise_analytics")
                                    }
                                )
                            },
                            bodyMetricsScreen = {
                                BodyMetricsScreen(viewModel = bodyMetricsViewModel)
                            },
                            activeWorkoutViewModel = activeWorkoutViewModel
                        )
                    }

                    composable("workout_detail") {
                        selectedWorkoutId?.let { workoutId ->
                            WorkoutDetailScreen(
                                sessionId = workoutId,
                                viewModel = historyViewModel,
                                onBack = { mainNavController.popBackStack() }
                            )
                        }
                    }

                    composable("template_edit") {
                        selectedTemplateId?.let { templateId ->
                            com.example.neokratos.ui.screen.templates.TemplateEditScreen(
                                templateId = templateId,
                                viewModel = templateEditViewModel,
                                onBack = { mainNavController.popBackStack() }
                            )
                        }
                    }

                    composable("exercise_analytics") {
                        ExerciseAnalyticsScreen(
                            viewModel = exerciseAnalyticsViewModel,
                            onBack = { mainNavController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}