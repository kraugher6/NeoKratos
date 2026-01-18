package com.example.neokratos.ui.screen.bodymetrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neokratos.data.local.entity.BodyMetricEntity
import com.example.neokratos.data.local.entity.BodyMetricType
import com.example.neokratos.data.local.entity.getDisplayValue
import com.example.neokratos.data.local.entity.getFormattedDate
import com.example.neokratos.data.repository.BodyMetricTimeRange
import com.example.neokratos.data.repository.WeightTrend
import java.text.SimpleDateFormat
import java.util.*

/**
 * Body Metrics screen.
 *
 * Shows:
 * - Current weight and stats
 * - Weight trend graph
 * - Weight history
 * - Body measurements
 * - Progress photos
 *
 * Concepts:
 * - Multiple metric types in one screen
 * - Tabs for different metric categories
 * - Time-series visualization
 * - Quick add/edit functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    viewModel: BodyMetricsViewModel,
    modifier: Modifier = Modifier
) {
    val weightStats by viewModel.weightStats.collectAsStateWithLifecycle()
    val weightTrend by viewModel.weightTrend.collectAsStateWithLifecycle()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsStateWithLifecycle()
    val weightHistory by viewModel.weightHistory.collectAsStateWithLifecycle()

    var showAddWeightDialog by remember { mutableStateOf(false) }
    var showAddMeasurementDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body Metrics") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showAddWeightDialog = true
                        1 -> showAddMeasurementDialog = true
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add metric")
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Weight") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Measurements") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Photos") }
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> WeightTab(
                    weightStats = weightStats,
                    weightTrend = weightTrend,
                    selectedTimeRange = selectedTimeRange,
                    weightHistory = weightHistory,
                    onTimeRangeChanged = { viewModel.selectTimeRange(it) },
                    onDeleteWeight = { viewModel.deleteMetric(it) }
                )
                1 -> MeasurementsTab(viewModel = viewModel)
                2 -> PhotosTab(viewModel = viewModel)
            }
        }
    }

    // Dialogs
    if (showAddWeightDialog) {
        AddWeightDialog(
            onConfirm = { weight, notes ->
                viewModel.logWeight(weight, notes)
                showAddWeightDialog = false
            },
            onDismiss = { showAddWeightDialog = false }
        )
    }

    if (showAddMeasurementDialog) {
        AddMeasurementDialog(
            onConfirm = { type, value, notes ->
                viewModel.logMeasurement(type, value, notes)
                showAddMeasurementDialog = false
            },
            onDismiss = { showAddMeasurementDialog = false }
        )
    }
}

/**
 * Weight tab content.
 */
@Composable
private fun WeightTab(
    weightStats: com.example.neokratos.data.repository.WeightStats?,
    weightTrend: WeightTrend?,
    selectedTimeRange: BodyMetricTimeRange,
    weightHistory: List<BodyMetricEntity>,
    onTimeRangeChanged: (BodyMetricTimeRange) -> Unit,
    onDeleteWeight: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Weight Card
        item {
            CurrentWeightCard(
                weightStats = weightStats,
                weightTrend = weightTrend
            )
        }

        // Time Range Selector
        item {
            TimeRangeSelector(
                selectedRange = selectedTimeRange,
                onRangeSelected = onTimeRangeChanged
            )
        }

        // Weight Graph Card
        item {
            WeightGraphCard(weightHistory = weightHistory)
        }

        // Weight History Section
        item {
            Text(
                text = "Weight History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (weightHistory.isEmpty()) {
            item {
                Text(
                    text = "No weight entries yet. Tap + to add your first measurement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(
                items = weightHistory,
                key = { it.id }
            ) { weight ->
                WeightHistoryCard(
                    weight = weight,
                    onDelete = { onDeleteWeight(weight.id) }
                )
            }
        }
    }
}

/**
 * Current weight summary card.
 */
@Composable
private fun CurrentWeightCard(
    weightStats: com.example.neokratos.data.repository.WeightStats?,
    weightTrend: WeightTrend?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (weightStats == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                // Current Weight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Weight",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (weightStats.currentWeight != null) {
                                "${String.format("%.1f", weightStats.currentWeight)} kg"
                            } else {
                                "No data"
                            },
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Trend indicator
                    if (weightTrend != null) {
                        TrendIndicator(trend = weightTrend)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "Total Change",
                        value = "${if (weightStats.totalChange >= 0) "+" else ""}${String.format("%.1f", weightStats.totalChange)} kg"
                    )
                    StatItem(
                        label = "7-Day Trend",
                        value = "${if (weightStats.sevenDayTrend >= 0) "+" else ""}${String.format("%.1f", weightStats.sevenDayTrend)} kg"
                    )
                    StatItem(
                        label = "Entries",
                        value = weightStats.measurementCount.toString()
                    )
                }
            }
        }
    }
}

/**
 * Trend indicator icon.
 */
@Composable
private fun TrendIndicator(trend: WeightTrend) {
    val (icon, color, text) = when (trend) {
        WeightTrend.GAINING -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            MaterialTheme.colorScheme.error,
            "Gaining"
        )
        WeightTrend.LOSING -> Triple(
            Icons.Default.TrendingDown,
            MaterialTheme.colorScheme.tertiary,
            "Losing"
        )
        WeightTrend.STABLE -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Stable"
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Small stat item.
 */
@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Time range selector.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRangeSelector(
    selectedRange: BodyMetricTimeRange,
    onRangeSelected: (BodyMetricTimeRange) -> Unit,
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
                selected = selectedRange == BodyMetricTimeRange.LAST_7_DAYS,
                onClick = { onRangeSelected(BodyMetricTimeRange.LAST_7_DAYS) },
                label = { Text("7 Days") }
            )
            FilterChip(
                selected = selectedRange == BodyMetricTimeRange.LAST_30_DAYS,
                onClick = { onRangeSelected(BodyMetricTimeRange.LAST_30_DAYS) },
                label = { Text("30 Days") }
            )
            FilterChip(
                selected = selectedRange == BodyMetricTimeRange.LAST_3_MONTHS,
                onClick = { onRangeSelected(BodyMetricTimeRange.LAST_3_MONTHS) },
                label = { Text("3 Months") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedRange == BodyMetricTimeRange.LAST_YEAR,
                onClick = { onRangeSelected(BodyMetricTimeRange.LAST_YEAR) },
                label = { Text("1 Year") }
            )
            FilterChip(
                selected = selectedRange == BodyMetricTimeRange.ALL_TIME,
                onClick = { onRangeSelected(BodyMetricTimeRange.ALL_TIME) },
                label = { Text("All Time") }
            )
        }
    }
}

/**
 * Weight graph card.
 * Shows simple data points for now.
 */
@Composable
private fun WeightGraphCard(
    weightHistory: List<BodyMetricEntity>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Weight Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (weightHistory.isEmpty()) {
                Text(
                    text = "No data to display",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Show data points
                weightHistory.take(10).forEach { weight ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = weight.getFormattedDate(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = weight.getDisplayValue(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

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
 * Single weight history entry card.
 */
@Composable
private fun WeightHistoryCard(
    weight: BodyMetricEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = weight.getDisplayValue(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = weight.getFormattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (weight.notes != null) {
                    Text(
                        text = weight.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Measurements tab content.
 */
@Composable
private fun MeasurementsTab(
    viewModel: BodyMetricsViewModel,
    modifier: Modifier = Modifier
) {
    val measurements by viewModel.allMeasurements.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Body Measurements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (measurements.isEmpty()) {
            item {
                Text(
                    text = "No measurements yet. Tap + to add your first measurement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(
                items = measurements,
                key = { it.id }
            ) { measurement ->
                MeasurementCard(
                    measurement = measurement,
                    onDelete = { viewModel.deleteMetric(measurement.id) }
                )
            }
        }
    }
}

/**
 * Single measurement card.
 */
@Composable
private fun MeasurementCard(
    measurement: BodyMetricEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = BodyMetricType.getDisplayName(measurement.type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${measurement.getDisplayValue()} • ${measurement.getFormattedDate()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (measurement.notes != null) {
                    Text(
                        text = measurement.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Photos tab content.
 */
@Composable
private fun PhotosTab(
    viewModel: BodyMetricsViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📸 Progress Photos",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Photo feature coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Dialog for adding weight.
 */
@Composable
private fun AddWeightDialog(
    onConfirm: (Float, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            weight = newValue
                        }
                    },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val w = weight.toFloatOrNull() ?: return@TextButton
                    onConfirm(w, notes.ifBlank { null })
                },
                enabled = weight.toFloatOrNull() != null
            ) {
                Text("Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for adding measurement.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMeasurementDialog(
    onConfirm: (BodyMetricType, Float, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(BodyMetricType.CHEST) }
    var value by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val measurementTypes = listOf(
        BodyMetricType.NECK,
        BodyMetricType.CHEST,
        BodyMetricType.WAIST,
        BodyMetricType.HIPS,
        BodyMetricType.BICEP_LEFT,
        BodyMetricType.BICEP_RIGHT,
        BodyMetricType.THIGH_LEFT,
        BodyMetricType.THIGH_RIGHT,
        BodyMetricType.CALF_LEFT,
        BodyMetricType.CALF_RIGHT
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Measurement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = BodyMetricType.getDisplayName(selectedType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Measurement Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        measurementTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(BodyMetricType.getDisplayName(type)) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            value = newValue
                        }
                    },
                    label = { Text("Value (cm)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = value.toFloatOrNull() ?: return@TextButton
                    onConfirm(selectedType, v, notes.ifBlank { null })
                },
                enabled = value.toFloatOrNull() != null
            ) {
                Text("Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}