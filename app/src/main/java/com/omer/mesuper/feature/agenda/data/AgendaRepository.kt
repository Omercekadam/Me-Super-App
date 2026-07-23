package com.omer.mesuper.feature.agenda.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.omer.mesuper.core.datastore.UserPrefsStore
import com.omer.mesuper.core.network.GitHubApi
import com.omer.mesuper.core.widget.MeSuperWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgendaRepository @Inject constructor(
    private val dao: AgendaDao,
    private val gitHubApi: GitHubApi,
    private val userPrefsStore: UserPrefsStore,
    @ApplicationContext private val context: Context,
) {
    private suspend fun refreshWidget() = MeSuperWidget().updateAll(context)

    val habits: Flow<List<HabitEntity>> = dao.observeHabits()
    val ticks: Flow<List<HabitTickEntity>> = dao.observeTicks()
    val tasks: Flow<List<TaskEntity>> = dao.observeTasks()
    val sessions: Flow<List<PomodoroSessionEntity>> = dao.observeSessions()
    val githubDays: Flow<List<GithubDayEntity>> = dao.observeGithubDays()

    suspend fun addHabit(name: String, emoji: String, colorHex: String) {
        dao.insertHabit(HabitEntity(name = name, emoji = emoji, colorHex = colorHex))
        refreshWidget()
    }

    suspend fun deleteHabit(id: Long) {
        dao.deleteHabit(id)
        refreshWidget()
    }

    /** Bugünkü tik durumunu tersine çevirir (yoksa ekler, varsa siler). */
    suspend fun toggleTick(habitId: Long, date: LocalDate = LocalDate.now()) {
        val alreadyTicked = dao.observeTicks().first().any { it.habitId == habitId && it.date == date }
        if (alreadyTicked) dao.untickHabit(habitId, date) else dao.tickHabit(HabitTickEntity(habitId = habitId, date = date))
        refreshWidget()
    }

    suspend fun addTask(title: String, dueDate: LocalDate? = null) {
        dao.insertTask(TaskEntity(title = title, dueDate = dueDate))
        refreshWidget()
    }

    suspend fun setTaskDone(task: TaskEntity, done: Boolean) {
        dao.updateTask(task.copy(isDone = done, doneAt = if (done) Instant.now() else null))
        refreshWidget()
    }

    suspend fun deleteTask(id: Long) {
        dao.deleteTask(id)
        refreshWidget()
    }

    suspend fun addSession(taskId: Long?, startedAt: Instant, durationMin: Int, completed: Boolean) =
        dao.insertSession(
            PomodoroSessionEntity(taskId = taskId, startedAt = startedAt, durationMin = durationMin, completed = completed)
        )

    /** GitHub önbelleğini tamamen yeni bir anlık görüntüyle değiştirir. */
    suspend fun replaceGithubDays(days: List<GithubDayEntity>) {
        dao.clearGithubDays()
        dao.upsertGithubDays(days)
    }

    /** Ayarlarda kayıtlı PAT + kullanıcı adıyla GitHub'dan commit takvimini çeker ve önbelleğe yazar. */
    suspend fun syncGithubContributions() {
        val pat = userPrefsStore.githubPat.first()
        val username = userPrefsStore.githubUsername.first()
        require(!pat.isNullOrBlank() && !username.isNullOrBlank()) {
            "GitHub PAT ve kullanıcı adı Ayarlar'dan girilmeli"
        }
        val days = gitHubApi.fetchContributionCalendar(pat, username)
        replaceGithubDays(days.map { GithubDayEntity(date = it.date, commitCount = it.count) })
    }
}
