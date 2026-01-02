package com.example.neokratos.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.neokratos.ui.navigation.BottomNavItem

@Composable
fun HomeScreen(
    templatesScreen: @Composable () -> Unit,
    manageTemplatesScreen: @Composable () -> Unit,
    historyScreen: @Composable () -> Unit
) {
    val navController = rememberNavController()

    // Lista degli item della BottomNav
    val items = BottomNavItem.items

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    // Otteniamo la destinazione corrente
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    // Determiniamo se l'item è selezionato
                    val isSelected = currentDestination?.hierarchy?.any {
                        it.route == item.id
                    } == true

                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.id) {
                                popUpTo(navController.graph.findStartDestination().id) {
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
        // NavHost per gestire i contenuti delle schermate
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Templates.id,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Templates.id) { templatesScreen() }
            composable(BottomNavItem.Manage.id) { manageTemplatesScreen() }
            composable(BottomNavItem.History.id) { historyScreen() }
        }
    }
}
