package com.example.neokratos.ui.navigation

/**
 * Navigation routes for the app.
 */
sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object ActiveWorkout : NavRoutes("active_workout")
    object History : NavRoutes("history")
    object Templates : NavRoutes("templates")
    object Analytics : NavRoutes("analytics")
    object Settings : NavRoutes("settings")
}