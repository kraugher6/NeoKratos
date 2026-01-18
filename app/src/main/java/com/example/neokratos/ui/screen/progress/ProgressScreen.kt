package com.example.neokratos.ui.screen.progress

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.neokratos.ui.screen.analytics.AnalyticsScreen
import com.example.neokratos.ui.screen.analytics.AnalyticsViewModel
import com.example.neokratos.ui.screen.bodymetrics.BodyMetricsScreen
import com.example.neokratos.ui.screen.bodymetrics.BodyMetricsViewModel
import com.example.neokratos.ui.screen.history.HistoryScreen
import com.example.neokratos.ui.screen.history.HistoryViewModel

/**
 * Combined Progress screen with 3 tabs:
 * - History: Past workouts
 * - Analytics: Stats and charts
 * - Body: Weight and measurements
 *
 * Simple, no bullshit. Swipe between tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    historyViewModel: HistoryViewModel,
    analyticsViewModel: AnalyticsViewModel,
    bodyMetricsViewModel: BodyMetricsViewModel,
    onWorkoutClick: (Long) -> Unit,
    onNavigateToExerciseAnalytics: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                // Compact header - no title, just tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "HISTORY",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "STATS",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "BODY",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> HistoryScreen(
                    viewModel = historyViewModel,
                    onWorkoutClick = onWorkoutClick
                )
                1 -> AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    onNavigateToExerciseAnalytics = onNavigateToExerciseAnalytics
                )
                2 -> BodyMetricsScreen(
                    viewModel = bodyMetricsViewModel
                )
            }
        }
    }
}