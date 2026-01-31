package com.example.neokratos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.neokratos.MainActivity
import com.example.neokratos.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Foreground Service for Rest Timer Notification.
 *
 * Features:
 * - Persistent ongoing notification during rest timer
 * - Shows exercise name and remaining time
 * - Updates every second
 * - Pause/Resume/Skip actions
 * - Vibration when timer completes
 * - Sound notification when timer completes
 * - Auto-dismiss when workout ends
 *
 * Lifecycle:
 * - Started when timer begins
 * - Updates every second
 * - Stopped when timer completed/skipped or workout ends
 */
class RestTimerService : Service() {

    companion object {
        private const val CHANNEL_ID = "rest_timer_channel"
        private const val NOTIFICATION_ID = 1001

        // Intent actions
        const val ACTION_START_TIMER = "com.example.neokratos.START_TIMER"
        const val ACTION_PAUSE_TIMER = "com.example.neokratos.PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "com.example.neokratos.RESUME_TIMER"
        const val ACTION_SKIP_TIMER = "com.example.neokratos.SKIP_TIMER"
        const val ACTION_UPDATE_TIMER = "com.example.neokratos.UPDATE_TIMER"
        const val ACTION_STOP_SERVICE = "com.example.neokratos.STOP_SERVICE"

        // Intent extras
        const val EXTRA_EXERCISE_NAME = "exercise_name"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        const val EXTRA_IS_PAUSED = "is_paused"

        // Vibration pattern: vibrate-pause-vibrate-pause-vibrate (in milliseconds)
        // Pattern: delay, vibrate, pause, vibrate, pause, vibrate
        private val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500, 200, 500)

        /**
         * Start timer service with initial state.
         */
        fun startTimer(
            context: Context,
            exerciseName: String,
            totalSeconds: Int,
            remainingSeconds: Int
        ) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(EXTRA_EXERCISE_NAME, exerciseName)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Update timer state (called every second from ViewModel).
         */
        fun updateTimer(
            context: Context,
            exerciseName: String,
            totalSeconds: Int,
            remainingSeconds: Int,
            isPaused: Boolean
        ) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_UPDATE_TIMER
                putExtra(EXTRA_EXERCISE_NAME, exerciseName)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_IS_PAUSED, isPaused)
            }
            context.startService(intent)
        }

        /**
         * Pause timer.
         */
        fun pauseTimer(context: Context) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_PAUSE_TIMER
            }
            context.startService(intent)
        }

        /**
         * Resume timer.
         */
        fun resumeTimer(context: Context) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_RESUME_TIMER
            }
            context.startService(intent)
        }

        /**
         * Skip/cancel timer.
         */
        fun skipTimer(context: Context) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_SKIP_TIMER
            }
            context.startService(intent)
        }

        /**
         * Stop service (called when workout ends).
         */
        fun stopService(context: Context) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }

    // State
    private var exerciseName: String = "Exercise"
    private var totalSeconds: Int = 0
    private var remainingSeconds: Int = 0
    private var isPaused: Boolean = false

    // Vibrator
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Notification Manager
    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: "Exercise"
                totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
                remainingSeconds = intent.getIntExtra(EXTRA_REMAINING_SECONDS, 0)
                isPaused = false

                startForeground(NOTIFICATION_ID, buildNotification())
            }

            ACTION_UPDATE_TIMER -> {
                exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME) ?: exerciseName
                totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                remainingSeconds = intent.getIntExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, isPaused)

                // Update notification
                notificationManager.notify(NOTIFICATION_ID, buildNotification())

                // If timer completed (0 seconds), trigger completion effects
                if (remainingSeconds == 0 && !isPaused) {
                    onTimerCompleted()
                }
            }

            ACTION_PAUSE_TIMER -> {
                isPaused = true
                notificationManager.notify(NOTIFICATION_ID, buildNotification())

                // Broadcast pause action to ViewModel
                sendBroadcast(Intent(ACTION_PAUSE_TIMER))
            }

            ACTION_RESUME_TIMER -> {
                isPaused = false
                notificationManager.notify(NOTIFICATION_ID, buildNotification())

                // Broadcast resume action to ViewModel
                sendBroadcast(Intent(ACTION_RESUME_TIMER))
            }

            ACTION_SKIP_TIMER -> {
                // Broadcast skip action to ViewModel
                sendBroadcast(Intent(ACTION_SKIP_TIMER))

                // Stop service
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        // If service gets killed by system, don't restart
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Create notification channel for rest timer.
     * Only called once on Android O+ (API 26+).
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rest Timer",
                NotificationManager.IMPORTANCE_LOW // Low importance = no sound for regular updates
            ).apply {
                description = "Shows rest timer between sets during workout"
                setShowBadge(false)
                enableVibration(false) // Disable vibration for regular updates
                setSound(null, null) // No sound for regular updates
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Build notification with current timer state.
     */
    private fun buildNotification(): Notification {
        // Intent to open app when notification is tapped
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause/Resume action
        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(this, RestTimerService::class.java).apply {
                action = ACTION_RESUME_TIMER
            }
            val resumePendingIntent = PendingIntent.getService(
                this,
                1,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Resume",
                resumePendingIntent
            )
        } else {
            val pauseIntent = Intent(this, RestTimerService::class.java).apply {
                action = ACTION_PAUSE_TIMER
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                1,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            )
        }

        // Skip action
        val skipIntent = Intent(this, RestTimerService::class.java).apply {
            action = ACTION_SKIP_TIMER
        }
        val skipPendingIntent = PendingIntent.getService(
            this,
            2,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val skipAction = NotificationCompat.Action(
            android.R.drawable.ic_delete,
            "Skip",
            skipPendingIntent
        )

        // Format time
        val timeText = formatTime(remainingSeconds)

        // Build notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer) // You'll need to add this icon
            .setContentTitle(if (isPaused) "Rest Timer (Paused)" else "Rest Timer")
            .setContentText("$exerciseName - $timeText")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true) // Cannot be dismissed by user
            .setOnlyAlertOnce(true) // Don't alert on updates
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .addAction(pauseResumeAction)
            .addAction(skipAction)
            .setProgress(totalSeconds, remainingSeconds, false)
            .build()
    }

    /**
     * Format seconds as MM:SS.
     */
    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, secs)
    }

    /**
     * Called when timer reaches 0.
     * Triggers vibration and sound notification.
     */
    private fun onTimerCompleted() {
        // Vibrate
        triggerVibration()

        // Play sound
        playCompletionSound()

        // Update notification to show completion
        val completionNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Rest Complete!")
            .setContentText("$exerciseName - Ready for next set")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for completion
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true) // Allow dismiss
            .build()

        notificationManager.notify(NOTIFICATION_ID, completionNotification)
    }

    /**
     * Trigger vibration pattern.
     * Pattern: vibrate 500ms, pause 200ms, vibrate 500ms, pause 200ms, vibrate 500ms
     */
    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(VIBRATION_PATTERN, -1) // -1 = don't repeat
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_PATTERN, -1)
        }
    }

    /**
     * Play system notification sound.
     * Uses default notification sound from Android system.
     */
    private fun playCompletionSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
                ringtone.volume = 1.0f
            }

            ringtone.play()
        } catch (e: Exception) {
            // If sound fails, just continue without sound
            e.printStackTrace()
        }
    }
}