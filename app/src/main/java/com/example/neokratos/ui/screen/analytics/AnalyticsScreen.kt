package com.example.neokratos.ui.screen.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neokratos.data.repository.TimeRange
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Analytics overview screen.
 *
 * Shows:
 * - Lifetime stats (total workouts, volume, avg duration)
 * - Workout frequency
 * - Time range selector
 * - Link to exercise-specific analytics
 *
 * Concepts:
 * - Statistics display with Cards
 * - Time range filtering
 * - Navigation to detail screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToExerciseAnalytics: () -> Unit
) {
    val lifetimeStats by viewModel.lifetimeStats.collectAsStateWithLifecycle()
    val frequencyStats by viewModel.frequencyStats.collectAsStateWithLifecycle()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                actions = {
                    IconButton(onClick = onNavigateToExerciseAnalytics) {
                        Icon(Icons.Default.TrendingUp, contentDescription = "Exercise Analytics")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lifetime Stats Section
            item {
                Text(
                    text = "Lifetime Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                LifetimeStatsCard(stats = lifetimeStats)
            }

            // Time Range Selector
            item {
                TimeRangeSelector(
                    selectedRange = selectedTimeRange,
                    onRangeSelected = { viewModel.selectTimeRange(it) }
                )
            }

            // Frequency Stats Section
            item {
                Text(
                    text = "Workout Frequency",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                FrequencyStatsCard(stats = frequencyStats)
            }

            // Volume Over Time Section
            item {
                Text(
                    text = "Volume Trends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                VolumeOverTimeCard(viewModel = viewModel)
            }

            // Exercise Analytics Button
            item {
                OutlinedButton(
                    onClick = onNavigateToExerciseAnalytics,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Exercise Analytics")
                }
            }
        }
    }
}

/**
 * Card displaying lifetime statistics.
 */
@Composable
private fun LifetimeStatsCard(
    stats: com.example.neokratos.data.repository.LifetimeStats?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (stats == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                // Total Workouts
                StatRow(
                    label = "Total Workouts",
                    value = stats.totalWorkouts.toString(),
                    icon = "🏋️"
                )

                Divider()

                // Total Volume
                StatRow(
                    label = "Total Volume",
                    value = "${formatVolume(stats.totalVolume)} kg",
                    icon = "💪"
                )

                Divider()

                // Average Duration
                StatRow(
                    label = "Avg Duration",
                    value = formatDuration(stats.averageDuration),
                    icon = "⏱️"
                )
            }
        }
    }
}

/**
 * Time range selector chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Time Range",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedRange == TimeRange.LAST_7_DAYS,
                onClick = { onRangeSelected(TimeRange.LAST_7_DAYS) },
                label = { Text("7 Days") }
            )
            FilterChip(
                selected = selectedRange == TimeRange.LAST_30_DAYS,
                onClick = { onRangeSelected(TimeRange.LAST_30_DAYS) },
                label = { Text("30 Days") }
            )
            FilterChip(
                selected = selectedRange == TimeRange.LAST_3_MONTHS,
                onClick = { onRangeSelected(TimeRange.LAST_3_MONTHS) },
                label = { Text("3 Months") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedRange == TimeRange.LAST_6_MONTHS,
                onClick = { onRangeSelected(TimeRange.LAST_6_MONTHS) },
                label = { Text("6 Months") }
            )
            FilterChip(
                selected = selectedRange == TimeRange.LAST_YEAR,
                onClick = { onRangeSelected(TimeRange.LAST_YEAR) },
                label = { Text("1 Year") }
            )
            FilterChip(
                selected = selectedRange == TimeRange.ALL_TIME,
                onClick = { onRangeSelected(TimeRange.ALL_TIME) },
                label = { Text("All Time") }
            )
        }
    }
}

/**
 * Card displaying frequency statistics.
 */
@Composable
private fun FrequencyStatsCard(
    stats: com.example.neokratos.data.repository.FrequencyStats?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (stats == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                StatRow(
                    label = "Total Workouts",
                    value = stats.totalWorkouts.toString(),
                    icon = "📊"
                )

                Divider()

                StatRow(
                    label = "Average per Week",
                    value = String.format("%.1f", stats.averagePerWeek),
                    icon = "📅"
                )

                Divider()

                // Date range display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "From",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDate(stats.rangeStart),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "To",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDate(stats.rangeEnd),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card displaying volume over time.
 * Shows simple list for now, will add chart later.
 */
@Composable
private fun VolumeOverTimeCard(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val volumeData by viewModel.volumeOverTime.collectAsStateWithLifecycle()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Volume by Week",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (volumeData.isEmpty()) {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Show last 8 weeks
                volumeData.takeLast(8).forEach { dataPoint ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDate(dataPoint.timestamp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${formatVolume(dataPoint.volume)} kg (${dataPoint.workoutCount} workouts)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // TODO: Add chart visualization here
            Text(
                text = "📊 Chart visualization coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Generic stat row with icon, label, and value.
 */
@Composable
private fun StatRow(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ===== HELPER FUNCTIONS =====

/**
 * Format volume with thousands separator.
 * Example: 15000.0 → "15,000"
 */
private fun formatVolume(volume: Float): String {
    return String.format("%,d", volume.toInt())
}

/**
 * Format duration in seconds to readable string.
 * Example: 3665 seconds → "1h 1m"
 */
private fun formatDuration(seconds: Long): String {
    val hours = TimeUnit.SECONDS.toHours(seconds)
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

/**
 * Format timestamp to readable date.
 * Example: 1704672000000 → "Jan 8, 2024"
 */
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return format.format(date)
}