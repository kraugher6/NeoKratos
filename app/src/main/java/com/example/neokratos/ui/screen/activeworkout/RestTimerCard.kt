package com.example.neokratos.ui.screen.activeworkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Card showing rest timer between sets.
 *
 * Features:
 * - Countdown timer
 * - Pause/Resume
 * - Skip/Cancel
 * - Visual progress indicator
 * - Sound/vibration on completion (TODO)
 *
 * Concepts:
 * - LaunchedEffect: runs side-effects when composable enters composition
 * - DisposableEffect: cleanup when composable leaves composition
 * - remember { mutableStateOf() }: persist state across recompositions
 */
@Composable
fun RestTimerCard(
    timerState: RestTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Show/hide card based on timer state
    AnimatedVisibility(
        visible = timerState !is RestTimerState.Idle,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            when (timerState) {
                is RestTimerState.Running -> {
                    RunningTimerContent(
                        totalSeconds = timerState.totalSeconds,
                        remainingSeconds = timerState.remainingSeconds,
                        onPause = onPause,
                        onSkip = onSkip
                    )
                }
                is RestTimerState.Paused -> {
                    PausedTimerContent(
                        totalSeconds = timerState.totalSeconds,
                        remainingSeconds = timerState.remainingSeconds,
                        onResume = onResume,
                        onSkip = onSkip
                    )
                }
                is RestTimerState.Completed -> {
                    CompletedTimerContent(onSkip = onSkip)
                }
                RestTimerState.Idle -> { /* Hidden */ }
            }
        }
    }
}

/**
 * Timer content when running.
 *
 * Uses LaunchedEffect to trigger countdown every second.
 */
@Composable
private fun RunningTimerContent(
    totalSeconds: Int,
    remainingSeconds: Int,
    onPause: () -> Unit,
    onSkip: () -> Unit
) {
    // Local state for countdown - updated every second
    var currentSeconds by remember(remainingSeconds) { mutableIntStateOf(remainingSeconds) }

    /**
     * LaunchedEffect with key 'currentSeconds':
     * - Runs when currentSeconds changes
     * - Cancels previous coroutine when key changes
     * - Perfect for countdown timers
     */
    LaunchedEffect(currentSeconds) {
        if (currentSeconds > 0) {
            delay(1000L) // Wait 1 second
            currentSeconds -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Rest Timer",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        // Large countdown display
        Text(
            text = formatTime(currentSeconds),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        // Progress indicator
        LinearProgressIndicator(
            progress = { (totalSeconds - currentSeconds).toFloat() / totalSeconds },
            modifier = Modifier.fillMaxWidth(),
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPause,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Pause, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pause")
            }

            Button(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Skip")
            }
        }
    }
}

/**
 * Timer content when paused.
 */
@Composable
private fun PausedTimerContent(
    totalSeconds: Int,
    remainingSeconds: Int,
    onResume: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Rest Timer (Paused)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Text(
            text = formatTime(remainingSeconds),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        LinearProgressIndicator(
            progress = { (totalSeconds - remainingSeconds).toFloat() / totalSeconds },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onResume,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Resume")
            }

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
        }
    }
}

/**
 * Timer content when completed.
 */
@Composable
private fun CompletedTimerContent(
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Rest Complete!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Ready for next set",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Button(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dismiss")
        }
    }
}

/**
 * Format seconds as MM:SS.
 * Example: 90 → "1:30"
 */
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(),"%d:%02d", minutes, secs)
}