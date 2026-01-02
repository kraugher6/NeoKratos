package com.example.neokratos.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    object Templates : BottomNavItem(
        id = "templates",
        label = "Templates",
        icon = Icons.AutoMirrored.Filled.List
    )

    object Manage : BottomNavItem(
        id = "manage",
        label = "Manage",
        icon = Icons.Default.Edit
    )

    object History : BottomNavItem(
        id = "history",
        label = "History",
        icon = Icons.Default.History
    )

    companion object {
        val items = listOf(Templates, Manage, History)
    }
}
