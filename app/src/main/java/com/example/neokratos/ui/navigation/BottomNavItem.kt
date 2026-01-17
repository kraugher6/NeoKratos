package com.example.neokratos.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation items - SIMPLIFIED to 4 tabs.
 *
 * Progress tab combines: History, Analytics, Body Metrics
 */
sealed class BottomNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    object Workout : BottomNavItem(
        id = "workout",
        label = "Workout",
        icon = Icons.Default.FitnessCenter
    )

    object Templates : BottomNavItem(
        id = "templates",
        label = "Templates",
        icon = Icons.AutoMirrored.Filled.List
    )

    object Exercises : BottomNavItem(
        id = "exercises",
        label = "Exercises",
        icon = Icons.Default.Edit
    )

    // NEW: Combined Progress tab
    object Progress : BottomNavItem(
        id = "progress",
        label = "Progress",
        icon = Icons.Default.Insights
    )

    companion object {
        val items = listOf(Workout, Templates, Exercises, Progress)
    }
}