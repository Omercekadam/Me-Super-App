package com.omer.mesuper.core.pomodoro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class PomodoroPhase { IDLE, RUNNING, PAUSED, FINISHED }

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.IDLE,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val taskId: Long? = null,
    val taskTitle: String? = null,
)

/** PomodoroService ile UI arasında paylaşılan tek gerçek durum kaynağı. */
@Singleton
class PomodoroController @Inject constructor() {
    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state

    fun update(transform: (PomodoroState) -> PomodoroState) {
        _state.value = transform(_state.value)
    }
}
