package com.example.neokratos.ui.navigation

sealed class NavRoutes(val route: String) {
    object Workout : NavRoutes("workout")
    object History : NavRoutes("history")
}

