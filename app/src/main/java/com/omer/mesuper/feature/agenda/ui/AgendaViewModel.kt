package com.omer.mesuper.feature.agenda.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omer.mesuper.core.python.AnalyticsEngine
import com.omer.mesuper.feature.agenda.data.AgendaRepository
import com.omer.mesuper.feature.agenda.data.GithubDayEntity
import com.omer.mesuper.feature.agenda.data.HabitEntity
import com.omer.mesuper.feature.agenda.data.HabitTickEntity
import com.omer.mesuper.feature.agenda.data.PomodoroSessionEntity
import com.omer.mesuper.feature.agenda.data.TaskEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Bir alışkanlığın ekranda gösterilecek tüm bilgisi: temel veri + Python'dan gelen streak. */
data class HabitRowUi(
    val id: Long,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val tickedToday: Boolean,
    /** En eskiden en yeniye (bugün dahil) son 7 günün tik durumu. */
    val last7Days: List<Boolean>,
)

data class AgendaUiState(
    val habits: List<HabitRowUi> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val pomodoroStats: PomodoroStats? = null,
    val githubStreak: GithubStreak? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

private data class RawData(
    val habits: List<HabitEntity>,
    val ticks: List<HabitTickEntity>,
    val tasks: List<TaskEntity>,
    val sessions: List<PomodoroSessionEntity>,
    val githubDays: List<GithubDayEntity>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val repo: AgendaRepository,
    private val engine: AnalyticsEngine,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val uiState: StateFlow<AgendaUiState> = combine(
        repo.habits, repo.ticks, repo.tasks, repo.sessions, repo.githubDays,
    ) { habits, ticks, tasks, sessions, githubDays ->
        RawData(habits, ticks, tasks, sessions, githubDays)
    }
        .mapLatest(::buildState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaUiState())

    private suspend fun buildState(raw: RawData): AgendaUiState {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        return runCatching {
            val habitStreaks: List<HabitStreak> = json.decodeFromString(
                engine.run("agenda.habitStreaks", buildJsonObject {
                    put("today", today.toString())
                    putJsonArray("habits") {
                        raw.habits.forEach { h -> addJsonObject { put("id", h.id); put("name", h.name) } }
                    }
                    putJsonArray("ticks") {
                        raw.ticks.forEach { t ->
                            addJsonObject { put("habitId", t.habitId); put("date", t.date.toString()) }
                        }
                    }
                }.toString())
            )
            val streakByHabit = habitStreaks.associateBy { it.habitId }
            val ticksByHabit = raw.ticks.groupBy({ it.habitId }, { it.date })

            val habitRows = raw.habits.map { h ->
                val streak = streakByHabit[h.id]
                val habitTicks = ticksByHabit[h.id].orEmpty().toSet()
                HabitRowUi(
                    id = h.id,
                    name = h.name,
                    emoji = h.emoji,
                    colorHex = h.colorHex,
                    currentStreak = streak?.currentStreak ?: 0,
                    longestStreak = streak?.longestStreak ?: 0,
                    tickedToday = streak?.tickedToday ?: false,
                    last7Days = (6 downTo 0).map { offset -> habitTicks.contains(today.minusDays(offset.toLong())) },
                )
            }

            val pomodoroStats: PomodoroStats = json.decodeFromString(
                engine.run("agenda.pomodoroStats", buildJsonObject {
                    put("today", today.toString())
                    putJsonArray("sessions") {
                        raw.sessions.forEach { s ->
                            addJsonObject {
                                put("date", s.startedAt.atZone(zone).toLocalDate().toString())
                                put("durationMin", s.durationMin)
                                put("completed", s.completed)
                            }
                        }
                    }
                }.toString())
            )

            val githubStreak: GithubStreak? = if (raw.githubDays.isEmpty()) null else json.decodeFromString(
                engine.run("agenda.githubStreak", buildJsonObject {
                    put("today", today.toString())
                    putJsonArray("days") {
                        raw.githubDays.forEach { d ->
                            addJsonObject { put("date", d.date.toString()); put("count", d.commitCount) }
                        }
                    }
                }.toString())
            )

            AgendaUiState(
                habits = habitRows,
                tasks = raw.tasks,
                pomodoroStats = pomodoroStats,
                githubStreak = githubStreak,
                loading = false,
            )
        }.getOrElse { e ->
            AgendaUiState(tasks = raw.tasks, loading = false, error = e.message ?: e.toString())
        }
    }

    // --- Kullanıcı eylemleri ---

    fun addHabit(name: String, emoji: String, colorHex: String) =
        viewModelScope.launch { repo.addHabit(name, emoji, colorHex) }

    fun deleteHabit(id: Long) = viewModelScope.launch { repo.deleteHabit(id) }

    fun toggleTick(habitId: Long) = viewModelScope.launch { repo.toggleTick(habitId) }

    fun addTask(title: String, dueDate: LocalDate?) = viewModelScope.launch { repo.addTask(title, dueDate) }

    fun setTaskDone(task: TaskEntity, done: Boolean) = viewModelScope.launch { repo.setTaskDone(task, done) }

    fun deleteTask(id: Long) = viewModelScope.launch { repo.deleteTask(id) }
}
