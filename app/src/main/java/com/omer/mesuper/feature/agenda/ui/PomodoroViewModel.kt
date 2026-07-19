package com.omer.mesuper.feature.agenda.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.omer.mesuper.core.pomodoro.PomodoroController
import com.omer.mesuper.core.pomodoro.PomodoroService
import com.omer.mesuper.core.pomodoro.PomodoroState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** PomodoroService'e komut gönderen ince katman; asıl durum PomodoroController'da tutulur. */
@HiltViewModel
class PomodoroViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    controller: PomodoroController,
) : ViewModel() {

    val state: StateFlow<PomodoroState> = controller.state

    fun start(minutes: Int, taskId: Long?, taskTitle: String?) {
        val intent = Intent(context, PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_START
            putExtra(PomodoroService.EXTRA_MINUTES, minutes)
            taskId?.let { putExtra(PomodoroService.EXTRA_TASK_ID, it) }
            taskTitle?.let { putExtra(PomodoroService.EXTRA_TASK_TITLE, it) }
        }
        context.startForegroundService(intent)
    }

    fun pause() = sendAction(PomodoroService.ACTION_PAUSE)
    fun resume() = sendAction(PomodoroService.ACTION_RESUME)
    fun stop() = sendAction(PomodoroService.ACTION_STOP)

    private fun sendAction(action: String) {
        context.startService(Intent(context, PomodoroService::class.java).apply { this.action = action })
    }
}
