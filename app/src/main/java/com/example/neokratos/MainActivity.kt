package com.example.neokratos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neokratos.data.local.GymDatabase
import com.example.neokratos.data.repository.AnalyticsRepository
import com.example.neokratos.data.repository.BodyMetricsRepository
import com.example.neokratos.data.repository.ExerciseRepository
import com.example.neokratos.data.repository.WorkoutSessionRepository
import com.example.neokratos.data.repository.WorkoutTemplateRepository
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutScreen
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutViewModel
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutViewModelFactory
import com.example.neokratos.ui.screen.analytics.AnalyticsViewModel
import com.example.neokratos.ui.screen.analytics.AnalyticsViewModelFactory
import com.example.neokratos.ui.screen.analytics.ExerciseAnalyticsScreen
import com.example.neokratos.ui.screen.analytics.ExerciseAnalyticsViewModel
import com.example.neokratos.ui.screen.bodymetrics.BodyMetricsViewModel
import com.example.neokratos.ui.screen.bodymetrics.BodyMetricsViewModelFactory
import com.example.neokratos.ui.screen.exercises.ExerciseLibraryScreen
import com.example.neokratos.ui.screen.exercises.ExerciseLibraryViewModel
import com.example.neokratos.ui.screen.exercises.ExerciseLibraryViewModelFactory
import com.example.neokratos.ui.screen.history.HistoryViewModel
import com.example.neokratos.ui.screen.history.HistoryViewModelFactory
import com.example.neokratos.ui.screen.history.WorkoutDetailScreen
import com.example.neokratos.ui.screen.home.HomeScreen
import com.example.neokratos.ui.screen.progress.ProgressScreen
import com.example.neokratos.ui.screen.templates.TemplateEditViewModel
import com.example.neokratos.ui.screen.templates.TemplateListScreen
import com.example.neokratos.ui.screen.templates.TemplateViewModel
import com.example.neokratos.ui.theme.NeoKratosTheme
import kotlinx.coroutines.launch

/**
 * Main Activity - UPDATED for notification permissions
 *
 * NEW: Requests POST_NOTIFICATIONS permission on Android 13+
 * Required for rest timer notifications to work
 */
class MainActivity : ComponentActivity() {

    /**
     * Permission launcher for POST_NOTIFICATIONS.
     * Registered before onCreate.
     */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted - notifications will work
            // No action needed, service will start when timer begins
        } else {
            // Permission denied - show explanation to user
            // Timer will still work but without notifications
            showNotificationPermissionDeniedExplanation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        requestNotificationPermission()

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
                val navController = rememberNavController()
                var selectedWorkoutId by remember { mutableStateOf<Long?>(null) }
                var selectedTemplateId by remember { mutableStateOf<Long?>(null) }

                // UPDATED: Pass application to factory
                val activeWorkoutViewModel: ActiveWorkoutViewModel = viewModel(
                    factory = ActiveWorkoutViewModelFactory(
                        application = application,
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
                            exercisesScreen = {
                                ExerciseLibraryScreen(viewModel = exerciseLibraryViewModel)
                            },
                            progressScreen = {
                                ProgressScreen(
                                    historyViewModel = historyViewModel,
                                    analyticsViewModel = analyticsViewModel,
                                    bodyMetricsViewModel = bodyMetricsViewModel,
                                    onWorkoutClick = { workoutId ->
                                        selectedWorkoutId = workoutId
                                        navController.navigate("workout_detail")
                                    },
                                    onNavigateToExerciseAnalytics = {
                                        navController.navigate("exercise_analytics")
                                    }
                                )
                            },
                            activeWorkoutViewModel = activeWorkoutViewModel
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

                    composable("exercise_analytics") {
                        ExerciseAnalyticsScreen(
                            viewModel = exerciseAnalyticsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    /**
     * Request notification permission on Android 13+ (API 33+).
     *
     * On older versions, permission is granted automatically.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // Permission already granted
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Nothing to do, notifications work
                }

                // Should show rationale (user denied before)
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation dialog before requesting again
                    showNotificationPermissionRationale()
                }

                // First time asking
                else -> {
                    // Request permission directly
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        // On Android 12 and below, permission is granted automatically
    }

    /**
     * Show rationale explaining why notification permission is needed.
     * Called when user denied permission before.
     */
    private fun showNotificationPermissionRationale() {
        // TODO: Show a dialog explaining the benefit of notifications
        // For now, just request again
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * Show explanation when user denies notification permission.
     * Timer will still work but without persistent notifications.
     */
    private fun showNotificationPermissionDeniedExplanation() {
        // TODO: Show a snackbar or dialog explaining:
        // "Rest timer notifications disabled. You can enable them in Settings."
        // The timer will still work in-app, just without the persistent notification
    }
}