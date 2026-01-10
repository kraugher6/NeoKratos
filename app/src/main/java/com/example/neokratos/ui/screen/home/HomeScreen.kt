package com.example.neokratos.ui.screen.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.neokratos.ui.navigation.BottomNavItem

/**
 * Main home screen with bottom navigation.
 *
 * Contains:
 * - Active Workout screen
 * - Templates list
 * - Manage templates
 * - History
 */
@Composable
fun HomeScreen(
    workoutScreen: @Composable () -> Unit,
    templatesScreen: @Composable () -> Unit,
    historyScreen: @Composable () -> Unit,
    exercisesScreen: @Composable () -> Unit,
    analyticsScreen: @Composable () -> Unit,
    bodyMetricsScreen: @Composable () -> Unit
) {
    val navController = rememberNavController()
    val items = BottomNavItem.items

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
                        onClick = {
                            navController.navigate(item.id) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
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