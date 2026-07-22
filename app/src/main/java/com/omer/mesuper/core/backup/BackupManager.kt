package com.omer.mesuper.core.backup

import androidx.room.withTransaction
import com.omer.mesuper.core.database.AppDatabase
import com.omer.mesuper.feature.activity.data.ActivityDao
import com.omer.mesuper.feature.activity.data.GameEntity
import com.omer.mesuper.feature.activity.data.GameSource
import com.omer.mesuper.feature.activity.data.GameStatus
import com.omer.mesuper.feature.activity.data.ManualPlayLogEntity
import com.omer.mesuper.feature.activity.data.MediaEntity
import com.omer.mesuper.feature.activity.data.MediaStatus
import com.omer.mesuper.feature.activity.data.MediaType
import com.omer.mesuper.feature.activity.data.RaceNoteEntity
import com.omer.mesuper.feature.activity.data.SteamSnapshotEntity
import com.omer.mesuper.feature.agenda.data.AgendaDao
import com.omer.mesuper.feature.agenda.data.GithubDayEntity
import com.omer.mesuper.feature.agenda.data.HabitEntity
import com.omer.mesuper.feature.agenda.data.HabitTickEntity
import com.omer.mesuper.feature.agenda.data.PomodoroSessionEntity
import com.omer.mesuper.feature.agenda.data.TaskEntity
import com.omer.mesuper.feature.finance.data.BudgetEntity
import com.omer.mesuper.feature.finance.data.CategoryEntity
import com.omer.mesuper.feature.finance.data.FinanceDao
import com.omer.mesuper.feature.finance.data.GoalContributionEntity
import com.omer.mesuper.feature.finance.data.GoalEntity
import com.omer.mesuper.feature.finance.data.PlanningDao
import com.omer.mesuper.feature.finance.data.SubscriptionEntity
import com.omer.mesuper.feature.finance.data.SubscriptionPeriod
import com.omer.mesuper.feature.finance.data.TransactionEntity
import com.omer.mesuper.feature.finance.data.TxType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Tüm veritabanının taşınabilir JSON temsili. Şema değişirse version artırılır. */
@Serializable
data class BackupDto(
    val version: Int = 1,
    val exportedAt: String,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
    val budgets: List<BudgetDto>,
    val subscriptions: List<SubscriptionDto>,
    val goals: List<GoalDto>,
    val goalContributions: List<ContributionDto>,
    val habits: List<HabitDto> = emptyList(),
    val habitTicks: List<HabitTickDto> = emptyList(),
    val tasks: List<TaskDto> = emptyList(),
    val pomodoroSessions: List<PomodoroSessionDto> = emptyList(),
    val githubDays: List<GithubDayDto> = emptyList(),
    val games: List<GameDto> = emptyList(),
    val steamSnapshots: List<SteamSnapshotDto> = emptyList(),
    val manualPlayLogs: List<ManualPlayLogDto> = emptyList(),
    val media: List<MediaDto> = emptyList(),
    val raceNotes: List<RaceNoteDto> = emptyList(),
)

@Serializable
data class CategoryDto(val id: Long, val name: String, val emoji: String, val colorHex: String, val type: String)

@Serializable
data class TransactionDto(
    val id: Long, val amountKurus: Long, val type: String, val categoryId: Long,
    val note: String, val occurredAt: String, val createdAtMs: Long,
)

@Serializable
data class BudgetDto(val id: Long, val categoryId: Long?, val monthlyLimitKurus: Long)

@Serializable
data class SubscriptionDto(
    val id: Long, val name: String, val amountKurus: Long, val billingDay: Int,
    val period: String, val billingMonth: Int?, val categoryId: Long?, val isActive: Boolean,
)

@Serializable
data class GoalDto(val id: Long, val name: String, val targetKurus: Long)

@Serializable
data class ContributionDto(val id: Long, val goalId: Long, val amountKurus: Long, val date: String)

@Serializable
data class HabitDto(val id: Long, val name: String, val emoji: String, val colorHex: String, val createdAt: String)

@Serializable
data class HabitTickDto(val id: Long, val habitId: Long, val date: String)

@Serializable
data class TaskDto(
    val id: Long, val title: String, val dueDate: String?,
    val isDone: Boolean, val doneAtMs: Long?, val createdAtMs: Long,
)

@Serializable
data class PomodoroSessionDto(
    val id: Long, val taskId: Long?, val startedAtMs: Long, val durationMin: Int, val completed: Boolean,
)

@Serializable
data class GithubDayDto(val date: String, val commitCount: Int)

@Serializable
data class GameDto(
    val id: Long, val source: String, val steamAppId: Long?, val rawgId: Long?,
    val name: String, val genres: String, val coverUrl: String?, val rating10: Int?,
    val status: String, val createdAtMs: Long,
)

@Serializable
data class SteamSnapshotDto(val id: Long, val steamAppId: Long, val date: String, val playtimeForeverMin: Long)

@Serializable
data class ManualPlayLogDto(val id: Long, val gameId: Long, val date: String, val minutes: Int)

@Serializable
data class MediaDto(
    val id: Long, val tmdbId: Long, val type: String, val title: String,
    val posterUrl: String?, val rating10: Int?, val status: String, val watchedAt: String?,
)

@Serializable
data class RaceNoteDto(
    val id: Long, val date: String, val sim: String, val track: String,
    val car: String, val bestLapMs: Long?, val setupNotes: String,
)

@Singleton
class BackupManager @Inject constructor(
    private val db: AppDatabase,
    private val financeDao: FinanceDao,
    private val planningDao: PlanningDao,
    private val agendaDao: AgendaDao,
    private val activityDao: ActivityDao,
) {
    // encodeDefaults: version alanı her yedekte açıkça yazılsın (ileri uyumluluk)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun exportJson(): String {
        val dto = BackupDto(
            exportedAt = Instant.now().toString(),
            categories = financeDao.dumpCategories().map {
                CategoryDto(it.id, it.name, it.emoji, it.colorHex, it.type.name)
            },
            transactions = financeDao.dumpTransactions().map {
                TransactionDto(
                    it.id, it.amountKurus, it.type.name, it.categoryId,
                    it.note, it.occurredAt.toString(), it.createdAt.toEpochMilli(),
                )
            },
            budgets = planningDao.dumpBudgets().map {
                BudgetDto(it.id, it.categoryId, it.monthlyLimitKurus)
            },
            subscriptions = planningDao.dumpSubscriptions().map {
                SubscriptionDto(
                    it.id, it.name, it.amountKurus, it.billingDay,
                    it.period.name, it.billingMonth, it.categoryId, it.isActive,
                )
            },
            goals = planningDao.dumpGoals().map { GoalDto(it.id, it.name, it.targetKurus) },
            goalContributions = planningDao.dumpContributions().map {
                ContributionDto(it.id, it.goalId, it.amountKurus, it.date.toString())
            },
            habits = agendaDao.dumpHabits().map {
                HabitDto(it.id, it.name, it.emoji, it.colorHex, it.createdAt.toString())
            },
            habitTicks = agendaDao.dumpTicks().map { HabitTickDto(it.id, it.habitId, it.date.toString()) },
            tasks = agendaDao.dumpTasks().map {
                TaskDto(it.id, it.title, it.dueDate?.toString(), it.isDone, it.doneAt?.toEpochMilli(), it.createdAt.toEpochMilli())
            },
            pomodoroSessions = agendaDao.dumpSessions().map {
                PomodoroSessionDto(it.id, it.taskId, it.startedAt.toEpochMilli(), it.durationMin, it.completed)
            },
            githubDays = agendaDao.dumpGithubDays().map { GithubDayDto(it.date.toString(), it.commitCount) },
            games = activityDao.dumpGames().map {
                GameDto(
                    it.id, it.source.name, it.steamAppId, it.rawgId, it.name, it.genres,
                    it.coverUrl, it.rating10, it.status.name, it.createdAt.toEpochMilli(),
                )
            },
            steamSnapshots = activityDao.dumpSteamSnapshots().map {
                SteamSnapshotDto(it.id, it.steamAppId, it.date.toString(), it.playtimeForeverMin)
            },
            manualPlayLogs = activityDao.dumpPlayLogs().map {
                ManualPlayLogDto(it.id, it.gameId, it.date.toString(), it.minutes)
            },
            media = activityDao.dumpMedia().map {
                MediaDto(it.id, it.tmdbId, it.type.name, it.title, it.posterUrl, it.rating10, it.status.name, it.watchedAt?.toString())
            },
            raceNotes = activityDao.dumpRaceNotes().map {
                RaceNoteDto(it.id, it.date.toString(), it.sim, it.track, it.car, it.bestLapMs, it.setupNotes)
            },
        )
        return json.encodeToString(BackupDto.serializer(), dto)
    }

    /** Mevcut TÜM veriyi siler ve yedekten geri yükler (tek transaction). */
    suspend fun importJson(raw: String) {
        val dto = json.decodeFromString(BackupDto.serializer(), raw)
        require(dto.version == 1) { "Desteklenmeyen yedek sürümü: ${dto.version}" }
        db.withTransaction {
            activityDao.clearPlayLogs()
            activityDao.clearGames()
            activityDao.clearSteamSnapshots()
            activityDao.clearMedia()
            activityDao.clearRaceNotes()

            agendaDao.clearSessions()
            agendaDao.clearTicks()
            agendaDao.clearTasks()
            agendaDao.clearHabits()
            agendaDao.clearGithubDays()
            planningDao.clearContributions()
            planningDao.clearGoals()
            planningDao.clearSubscriptions()
            planningDao.clearBudgets()
            financeDao.clearTransactions()
            financeDao.clearCategories()

            financeDao.insertCategories(dto.categories.map {
                CategoryEntity(it.id, it.name, it.emoji, it.colorHex, TxType.valueOf(it.type))
            })
            financeDao.insertTransactions(dto.transactions.map {
                TransactionEntity(
                    id = it.id,
                    amountKurus = it.amountKurus,
                    type = TxType.valueOf(it.type),
                    categoryId = it.categoryId,
                    note = it.note,
                    occurredAt = LocalDate.parse(it.occurredAt),
                    createdAt = Instant.ofEpochMilli(it.createdAtMs),
                )
            })
            planningDao.insertBudgets(dto.budgets.map {
                BudgetEntity(it.id, it.categoryId, it.monthlyLimitKurus)
            })
            planningDao.insertSubscriptions(dto.subscriptions.map {
                SubscriptionEntity(
                    id = it.id, name = it.name, amountKurus = it.amountKurus,
                    billingDay = it.billingDay, period = SubscriptionPeriod.valueOf(it.period),
                    billingMonth = it.billingMonth, categoryId = it.categoryId, isActive = it.isActive,
                )
            })
            planningDao.insertGoals(dto.goals.map { GoalEntity(it.id, it.name, it.targetKurus) })
            planningDao.insertContributions(dto.goalContributions.map {
                GoalContributionEntity(it.id, it.goalId, it.amountKurus, LocalDate.parse(it.date))
            })

            agendaDao.insertHabits(dto.habits.map {
                HabitEntity(it.id, it.name, it.emoji, it.colorHex, LocalDate.parse(it.createdAt))
            })
            agendaDao.insertTicks(dto.habitTicks.map { HabitTickEntity(it.id, it.habitId, LocalDate.parse(it.date)) })
            agendaDao.insertTasks(dto.tasks.map {
                TaskEntity(
                    id = it.id, title = it.title, dueDate = it.dueDate?.let(LocalDate::parse),
                    isDone = it.isDone, doneAt = it.doneAtMs?.let(Instant::ofEpochMilli),
                    createdAt = Instant.ofEpochMilli(it.createdAtMs),
                )
            })
            agendaDao.insertSessions(dto.pomodoroSessions.map {
                PomodoroSessionEntity(it.id, it.taskId, Instant.ofEpochMilli(it.startedAtMs), it.durationMin, it.completed)
            })
            agendaDao.upsertGithubDays(dto.githubDays.map { GithubDayEntity(LocalDate.parse(it.date), it.commitCount) })

            activityDao.insertGames(dto.games.map {
                GameEntity(
                    id = it.id, source = GameSource.valueOf(it.source), steamAppId = it.steamAppId, rawgId = it.rawgId,
                    name = it.name, genres = it.genres, coverUrl = it.coverUrl, rating10 = it.rating10,
                    status = GameStatus.valueOf(it.status), createdAt = Instant.ofEpochMilli(it.createdAtMs),
                )
            })
            activityDao.insertSteamSnapshots(dto.steamSnapshots.map {
                SteamSnapshotEntity(it.id, it.steamAppId, LocalDate.parse(it.date), it.playtimeForeverMin)
            })
            activityDao.insertPlayLogs(dto.manualPlayLogs.map {
                ManualPlayLogEntity(it.id, it.gameId, LocalDate.parse(it.date), it.minutes)
            })
            activityDao.insertMediaList(dto.media.map {
                MediaEntity(
                    id = it.id, tmdbId = it.tmdbId, type = MediaType.valueOf(it.type), title = it.title,
                    posterUrl = it.posterUrl, rating10 = it.rating10, status = MediaStatus.valueOf(it.status),
                    watchedAt = it.watchedAt?.let(LocalDate::parse),
                )
            })
            activityDao.insertRaceNotes(dto.raceNotes.map {
                RaceNoteEntity(it.id, LocalDate.parse(it.date), it.sim, it.track, it.car, it.bestLapMs, it.setupNotes)
            })
        }
    }
}
