package com.example.neokratos.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Templates : BottomNavItem(
        NavRoutes.Workout.route,
        "Templates",
        Icons.Default.List
    )

    object Manage : BottomNavItem(
        NavRoutes.Workout.route,
        "Manage",
        Icons.Default.Edit
    )

    object History : BottomNavItem(
        NavRoutes.History.route,
        "History",
        Icons.Default.History
    )
}
