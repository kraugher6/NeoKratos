package com.example.neokratos.ui.screen.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.neokratos.ui.navigation.BottomNavItem
import com.example.neokratos.ui.screen.activeworkout.ActiveWorkoutViewModel

/**
 * Main home screen with bottom navigation.
 *
 * FIX 2: Disables navigation when workout is active.
 * User must finish or cancel workout before navigating.
 */
@Composable
fun HomeScreen(
    workoutScreen: @Composable () -> Unit,
    templatesScreen: @Composable () -> Unit,
    historyScreen: @Composable () -> Unit,
    exercisesScreen: @Composable () -> Unit,
    analyticsScreen: @Composable () -> Unit,
    bodyMetricsScreen: @Composable () -> Unit,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    val navController = rememberNavController()
    val items = BottomNavItem.items

    // FIX 2: Observe active workout state to block navigation
    val activeWorkout by activeWorkoutViewModel.activeWorkout.collectAsStateWithLifecycle()
    val hasActiveWorkout = activeWorkout != null

    // FIX: Auto-navigate to Workout tab when workout starts
    LaunchedEffect(hasActiveWorkout) {
        if (hasActiveWorkout) {
            navController.navigate(BottomNavItem.Workout.id) {
                popUpTo(BottomNavItem.Workout.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val isSelected = currentDestination?.route == item.id

                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = isSelected,
                        // FIX 2: Disable navigation if workout active and not on workout tab
                        enabled = !hasActiveWorkout || item.id == BottomNavItem.Workout.id,
                        onClick = {
                            // FIX 2: Only allow navigation if no active workout or navigating to workout
                            if (!hasActiveWorkout || item.id == BottomNavItem.Workout.id) {
                                navController.navigate(item.id) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Workout.id,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Workout.id) { workoutScreen() }
            composable(BottomNavItem.Templates.id) { templatesScreen() }
            composable(BottomNavItem.History.id) { historyScreen() }
            composable(BottomNavItem.Exercises.id) { exercisesScreen() }
            composable(BottomNavItem.Analytics.id) { analyticsScreen() }
            composable(BottomNavItem.BodyMetrics.id) { bodyMetricsScreen() }
        }
    }
}