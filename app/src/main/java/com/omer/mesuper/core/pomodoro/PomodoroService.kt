package com.omer.mesuper.core.pomodoro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.omer.mesuper.MainActivity
import com.omer.mesuper.feature.agenda.data.AgendaRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Ekransız da çalışan pomodoro sayacı. Durum [PomodoroController] üzerinden
 * UI ile paylaşılır; servis sadece sayaç/bildirim yaşam döngüsünü yönetir.
 */
@AndroidEntryPoint
class PomodoroService : Service() {

    @Inject lateinit var controller: PomodoroController
    @Inject lateinit var repository: AgendaRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var sessionStartedAt: Instant? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSession(
                minutes = intent.getIntExtra(EXTRA_MINUTES, 25),
                taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it != -1L },
                taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE),
            )
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession(minutes: Int, taskId: Long?, taskTitle: String?) {
        sessionStartedAt = Instant.now()
        controller.update {
            PomodoroState(
                phase = PomodoroPhase.RUNNING,
                totalSeconds = minutes * 60,
                remainingSeconds = minutes * 60,
                taskId = taskId,
                taskTitle = taskTitle,
            )
        }
        ensureChannel()
        startForegroundCompat(buildNotification(controller.state.value))
        runTicker()
    }

    private fun runTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(1000)
                val current = controller.state.value
                if (current.phase == PomodoroPhase.PAUSED) continue
                if (current.phase != PomodoroPhase.RUNNING) break
                val next = current.remainingSeconds - 1
                if (next <= 0) {
                    controller.update { it.copy(phase = PomodoroPhase.FINISHED, remainingSeconds = 0) }
                    onSessionCompleted()
                    break
                } else {
                    controller.update { it.copy(remainingSeconds = next) }
                    updateNotification()
                }
            }
        }
    }

    private fun pause() {
        controller.update { it.copy(phase = PomodoroPhase.PAUSED) }
        updateNotification()
    }

    private fun resume() {
        controller.update { it.copy(phase = PomodoroPhase.RUNNING) }
        updateNotification()
        if (tickJob?.isActive != true) runTicker()
    }

    private fun onSessionCompleted() {
        val state = controller.state.value
        scope.launch {
            repository.addSession(
                taskId = state.taskId,
                startedAt = sessionStartedAt ?: Instant.now(),
                durationMin = state.totalSeconds / 60,
                completed = true,
            )
        }
        updateNotification()
        finishService()
    }

    private fun stopSession() {
        val state = controller.state.value
        val elapsedMin = (state.totalSeconds - state.remainingSeconds) / 60
        if (elapsedMin > 0) {
            scope.launch {
                repository.addSession(
                    taskId = state.taskId,
                    startedAt = sessionStartedAt ?: Instant.now(),
                    durationMin = elapsedMin,
                    completed = false,
                )
            }
        }
        controller.update { PomodoroState() }
        finishService()
    }

    private fun finishService() {
        tickJob?.cancel()
        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(controller.state.value))
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: PomodoroState): Notification {
        val minutes = state.remainingSeconds / 60
        val seconds = state.remainingSeconds % 60
        val taskSuffix = state.taskTitle?.let { " — $it" } ?: ""
        val contentText = when (state.phase) {
            PomodoroPhase.FINISHED -> "Tamamlandı 🎉"
            PomodoroPhase.PAUSED -> "Duraklatıldı — %02d:%02d$taskSuffix".format(minutes, seconds)
            else -> "%02d:%02d kaldı$taskSuffix".format(minutes, seconds)
        }

        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pomodoro")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(state.phase == PomodoroPhase.RUNNING)
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Pomodoro Sayacı", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object {
        const val ACTION_START = "com.omer.mesuper.pomodoro.START"
        const val ACTION_PAUSE = "com.omer.mesuper.pomodoro.PAUSE"
        const val ACTION_RESUME = "com.omer.mesuper.pomodoro.RESUME"
        const val ACTION_STOP = "com.omer.mesuper.pomodoro.STOP"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TASK_TITLE = "taskTitle"
        private const val CHANNEL_ID = "pomodoro"
        private const val NOTIFICATION_ID = 42
    }
}
