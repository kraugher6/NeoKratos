package com.example.neokratos.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation items.
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

    object History : BottomNavItem(
        id = "history",
        label = "History",
        icon = Icons.Default.History
    )

    object Exercises : BottomNavItem(
        id = "exercises",
        label = "Exercises",
        icon = Icons.Default.Edit
    )

    // NEW: Analytics tab
    object Analytics : BottomNavItem(
        id = "analytics",
        label = "Analytics",
        icon = Icons.Default.Insights
    )

    // NEW: Body Metrics tab
    object BodyMetrics : BottomNavItem(
        id = "body_metrics",
        label = "Body",
        icon = Icons.Default.MonitorWeight
    )

    companion object {
        val items = listOf(Workout, Templates, History, Exercises, Analytics, BodyMetrics)
    }
}