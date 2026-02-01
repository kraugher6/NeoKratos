package com.example.neokratos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.neokratos.MainActivity
import com.example.neokratos.R
import java.util.Locale

/**
 * Foreground Service for Rest Timer Notification.
 *
 * ✅ ALL 7 FIXES IMPLEMENTED:
 * 1. ✅ Auto-dismiss card integrata dopo 3 secondi
 * 2. ✅ Dismiss sincronizzato app ↔ notifica
 * 3. ✅ Notifica sempre collapsabile con tutti i dettagli
 * 4. ✅ Notifica NON silenziata (HIGH priority)
 * 5. ✅ Notifica appare nella status bar (zona notch)
 * 6. ✅ Pulsanti pause/resume/skip sincronizzati correttamente
 * 7. ✅ Suono solo quando finisce timer (non quando appare)
 */
class RestTimerService : Service() {

    companion object {
        private const val CHANNEL_ID = "rest_timer_channel"
        private const val NOTIFICATION_ID = 1001

        // Service actions
        const val ACTION_START_TIMER = "com.example.neokratos.START_TIMER"
        const val ACTION_PAUSE_TIMER = "com.example.neokratos.PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "com.example.neokratos.RESUME_TIMER"
        const val ACTION_SKIP_TIMER = "com.example.neokratos.SKIP_TIMER"
        const val ACTION_UPDATE_TIMER = "com.example.neokratos.UPDATE_TIMER"
        const val ACTION_STOP_SERVICE = "com.example.neokratos.STOP_SERVICE"

        // Broadcasts to ViewModel - FIXED: Match ViewModel receiver filter
        const val BROADCAST_PAUSE_TIMER = "com.example.neokratos.VM_PAUSE_TIMER"
        const val BROADCAST_RESUME_TIMER = "com.example.neokratos.VM_RESUME_TIMER"
        const val BROADCAST_SKIP_TIMER = "com.example.neokratos.VM_SKIP_TIMER"

        const val EXTRA_EXERCISE_NAME = "exercise_name"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        const val EXTRA_IS_PAUSED = "is_paused"

        private val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500, 200, 500)

        fun startTimer(context: Context, exerciseName: String, totalSeconds: Int, remainingSeconds: Int) {
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

        fun updateTimer(context: Context, exerciseName: String, totalSeconds: Int, remainingSeconds: Int, isPaused: Boolean) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                action = ACTION_UPDATE_TIMER
                putExtra(EXTRA_EXERCISE_NAME, exerciseName)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_IS_PAUSED, isPaused)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            context.startService(Intent(context, RestTimerService::class.java).apply {
                action = ACTION_STOP_SERVICE
            })
        }
    }

    private var exerciseName = "Exercise"
    private var totalSeconds = 0
    private var remainingSeconds = 0
    private var isPaused = false

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val notificationManager by lazy {
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
                notificationManager.notify(NOTIFICATION_ID, buildNotification())

                if (remainingSeconds == 0 && !isPaused) {
                    onTimerCompleted()
                }
            }
            ACTION_PAUSE_TIMER -> {
                isPaused = true
                notificationManager.notify(NOTIFICATION_ID, buildNotification())
                sendBroadcast(Intent(BROADCAST_PAUSE_TIMER))
            }
            ACTION_RESUME_TIMER -> {
                isPaused = false
                notificationManager.notify(NOTIFICATION_ID, buildNotification())
                sendBroadcast(Intent(BROADCAST_RESUME_TIMER))
            }
            ACTION_SKIP_TIMER -> {
                sendBroadcast(Intent(BROADCAST_SKIP_TIMER))
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * ✅ FIX 4 & 5: HIGH priority channel for non-silent notification in status bar
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Rest Timer", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Rest timer between sets"
                    setShowBadge(true)
                    enableVibration(false) // Manual vibration on completion
                    setSound(null, null)    // Manual sound on completion
                }
            )
        }
    }

    /**
     * ✅ FIX 3: BigTextStyle for always-expanded notification with all details
     * ✅ FIX 5: setCategory(ALARM) + HIGH priority → shows in status bar near notch
     * ✅ FIX 6: Actions send to service, which broadcasts to ViewModel
     */
    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (isPaused) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Resume",
                PendingIntent.getService(
                    this, 1,
                    Intent(this, RestTimerService::class.java).apply { action = ACTION_RESUME_TIMER },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pause",
                PendingIntent.getService(
                    this, 1,
                    Intent(this, RestTimerService::class.java).apply { action = ACTION_PAUSE_TIMER },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        val skipAction = NotificationCompat.Action(
            android.R.drawable.ic_delete, "Skip",
            PendingIntent.getService(
                this, 2,
                Intent(this, RestTimerService::class.java).apply { action = ACTION_SKIP_TIMER },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val timeText = String.format(Locale.getDefault(), "%d:%02d", remainingSeconds / 60, remainingSeconds % 60)
        val title = if (isPaused) "Rest Timer (Paused)" else "Rest Timer"
        val content = "$exerciseName - $timeText"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content).setBigContentTitle(title))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(pauseResumeAction)
            .addAction(skipAction)
            .setProgress(totalSeconds, totalSeconds - remainingSeconds, false)
            .setShowWhen(false)
            .build()
    }

    /**
     * ✅ FIX 1 & 2 & 7: Auto-dismiss after 3s, vibration + sound ONLY here
     */
    private fun onTimerCompleted() {
        triggerVibration()
        playCompletionSound()

        notificationManager.notify(NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Rest Complete!")
                .setContentText("$exerciseName - Ready for next set")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$exerciseName - Ready for next set")
                    .setBigContentTitle("✓ Rest Complete!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
        )

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }, 3000)
    }

    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, -1))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(VIBRATION_PATTERN, -1)
        }
    }

    private fun playCompletionSound() {
        try {
            RingtoneManager.getRingtone(applicationContext,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = false
                    volume = 1.0f
                }
                play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}