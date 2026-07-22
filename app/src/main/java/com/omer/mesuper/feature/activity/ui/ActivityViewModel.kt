package com.omer.mesuper.feature.activity.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omer.mesuper.core.network.RawgGameResult
import com.omer.mesuper.core.network.TmdbMediaKind
import com.omer.mesuper.core.network.TmdbSearchResult
import com.omer.mesuper.core.python.AnalyticsEngine
import com.omer.mesuper.feature.agenda.data.AgendaRepository
import com.omer.mesuper.feature.agenda.data.PomodoroSessionEntity
import com.omer.mesuper.feature.activity.data.ActivityRepository
import com.omer.mesuper.feature.activity.data.GameEntity
import com.omer.mesuper.feature.activity.data.GameSource
import com.omer.mesuper.feature.activity.data.GameStatus
import com.omer.mesuper.feature.activity.data.ManualPlayLogEntity
import com.omer.mesuper.feature.activity.data.MediaEntity
import com.omer.mesuper.feature.activity.data.MediaType
import com.omer.mesuper.feature.activity.data.RaceNoteEntity
import com.omer.mesuper.feature.activity.data.SteamSnapshotEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

/** Bir oyunun ekranda gösterilecek tüm bilgisi: temel veri + son 7 günün oynama süresi. */
data class GameRowUi(
    val id: Long,
    val name: String,
    val genres: String,
    val coverUrl: String?,
    val status: GameStatus,
    val source: GameSource,
    val rating10: Int?,
    val minutesLast7Days: Int,
)

data class ActivityUiState(
    val games: List<GameRowUi> = emptyList(),
    val media: List<MediaEntity> = emptyList(),
    val raceNotes: List<RaceNoteEntity> = emptyList(),
    val genreBreakdown: List<GenreMinutes> = emptyList(),
    val weeklyBalance: WeeklyBalance? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

private data class ActivityRawData(
    val games: List<GameEntity>,
    val snapshots: List<SteamSnapshotEntity>,
    val playLogs: List<ManualPlayLogEntity>,
    val media: List<MediaEntity>,
    val raceNotes: List<RaceNoteEntity>,
)

private data class RawData(
    val activity: ActivityRawData,
    val pomodoroSessions: List<PomodoroSessionEntity>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repo: ActivityRepository,
    private val agendaRepository: AgendaRepository,
    private val engine: AnalyticsEngine,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _actionStatus = MutableStateFlow<String?>(null)
    val actionStatus: StateFlow<String?> = _actionStatus

    val uiState: StateFlow<ActivityUiState> = combine(
        combine(repo.games, repo.steamSnapshots, repo.playLogs, repo.media, repo.raceNotes) { games, snapshots, playLogs, media, raceNotes ->
            ActivityRawData(games, snapshots, playLogs, media, raceNotes)
        },
        agendaRepository.sessions,
    ) { activityRaw, sessions -> RawData(activityRaw, sessions) }
        .mapLatest(::buildState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    private suspend fun buildState(raw: RawData): ActivityUiState {
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)
        val zone = ZoneId.systemDefault()

        return runCatching {
            val steamDiffs: List<SteamDailyMinutes> = json.decodeFromString(
                engine.run("activity.steamPlaytimeDiff", buildJsonObject {
                    putJsonArray("snapshots") {
                        raw.activity.snapshots.forEach { s ->
                            addJsonObject {
                                put("steamAppId", s.steamAppId)
                                put("date", s.date.toString())
                                put("playtimeForeverMin", s.playtimeForeverMin)
                            }
                        }
                    }
                }.toString())
            )

            // Son 7 günün oyun dakikaları: elle log + Steam farkı, hem toplamda hem oyun bazında.
            val steamAppToGameId = raw.activity.games.mapNotNull { g -> g.steamAppId?.let { it to g.id } }.toMap()
            val gameMinutesByDate = mutableMapOf<String, Int>()
            val gameMinutesLast7 = mutableMapOf<Long, Int>()

            raw.activity.playLogs.forEach { log ->
                gameMinutesByDate[log.date.toString()] = (gameMinutesByDate[log.date.toString()] ?: 0) + log.minutes
                if (log.date in weekStart..today) {
                    gameMinutesLast7[log.gameId] = (gameMinutesLast7[log.gameId] ?: 0) + log.minutes
                }
            }
            steamDiffs.forEach { diff ->
                gameMinutesByDate[diff.date] = (gameMinutesByDate[diff.date] ?: 0) + diff.minutesPlayed
                val diffDate = LocalDate.parse(diff.date)
                val gameId = steamAppToGameId[diff.steamAppId]
                if (gameId != null && diffDate in weekStart..today) {
                    gameMinutesLast7[gameId] = (gameMinutesLast7[gameId] ?: 0) + diff.minutesPlayed
                }
            }

            val gameRows = raw.activity.games.map { g ->
                GameRowUi(
                    id = g.id,
                    name = g.name,
                    genres = g.genres,
                    coverUrl = g.coverUrl,
                    status = g.status,
                    source = g.source,
                    rating10 = g.rating10,
                    minutesLast7Days = gameMinutesLast7[g.id] ?: 0,
                )
            }

            val genreBreakdown: List<GenreMinutes> = json.decodeFromString(
                engine.run("activity.genreBreakdown", buildJsonObject {
                    put("from", weekStart.toString())
                    put("to", today.toString())
                    putJsonArray("games") {
                        raw.activity.games.forEach { g -> addJsonObject { put("id", g.id); put("genres", g.genres) } }
                    }
                    putJsonArray("manualLogs") {
                        raw.activity.playLogs.forEach { log ->
                            addJsonObject { put("gameId", log.gameId); put("date", log.date.toString()); put("minutes", log.minutes) }
                        }
                    }
                    putJsonArray("steamMinutes") {
                        steamDiffs.forEach { d ->
                            addJsonObject { put("steamAppId", d.steamAppId); put("date", d.date); put("minutesPlayed", d.minutesPlayed) }
                        }
                    }
                    buildJsonObject { raw.activity.games.forEach { g -> g.steamAppId?.let { put(g.id.toString(), it) } } }
                        .let { obj -> put("gameSteamAppIds", obj) }
                }.toString())
            )

            val weeklyBalance: WeeklyBalance = json.decodeFromString(
                engine.run("activity.weeklyBalance", buildJsonObject {
                    put("today", today.toString())
                    put("gameMinutesByDate", buildJsonObject { gameMinutesByDate.forEach { (d, m) -> put(d, m) } })
                    putJsonArray("pomodoroSessions") {
                        raw.pomodoroSessions.forEach { s ->
                            addJsonObject {
                                put("date", s.startedAt.atZone(zone).toLocalDate().toString())
                                put("durationMin", s.durationMin)
                                put("completed", s.completed)
                            }
                        }
                    }
                }.toString())
            )

            ActivityUiState(
                games = gameRows,
                media = raw.activity.media,
                raceNotes = raw.activity.raceNotes,
                genreBreakdown = genreBreakdown,
                weeklyBalance = weeklyBalance,
                loading = false,
            )
        }.getOrElse { e ->
            ActivityUiState(media = raw.activity.media, raceNotes = raw.activity.raceNotes, loading = false, error = e.message ?: e.toString())
        }
    }

    // --- Oyunlar ---

    fun addManualGame(name: String, genres: String, status: GameStatus) =
        viewModelScope.launch { repo.addManualGame(name, genres, status) }

    fun addRawgGame(result: RawgGameResult, status: GameStatus) =
        viewModelScope.launch { repo.addRawgGame(result.rawgId, result.name, result.genres, result.coverUrl, status) }

    fun rateGame(gameId: Long, rating10: Int) = viewModelScope.launch { repo.rateGame(gameId, rating10) }

    fun setGameStatus(gameId: Long, status: GameStatus) = viewModelScope.launch { repo.setGameStatus(gameId, status) }

    fun deleteGame(id: Long) = viewModelScope.launch { repo.deleteGame(id) }

    fun addPlayLog(gameId: Long, date: LocalDate, minutes: Int) =
        viewModelScope.launch { repo.addPlayLog(gameId, date, minutes) }

    fun refreshSteamLibrary() {
        viewModelScope.launch {
            _actionStatus.value = runCatching {
                repo.syncSteamLibrary()
                "✅ Steam kütüphanesi güncellendi"
            }.getOrElse { "❌ ${it.message}" }
        }
    }

    suspend fun searchRawgGames(query: String): Result<List<RawgGameResult>> = runCatching { repo.searchRawgGames(query) }

    // --- Film/Dizi ---

    suspend fun searchTmdb(kind: TmdbMediaKind, query: String): Result<List<TmdbSearchResult>> =
        runCatching { repo.searchTmdb(kind, query) }

    fun addMedia(result: TmdbSearchResult, type: MediaType) =
        viewModelScope.launch { repo.addMedia(result.tmdbId, type, result.title, result.posterUrl) }

    fun markMediaWatched(media: MediaEntity, rating10: Int?) =
        viewModelScope.launch { repo.markMediaWatched(media, rating10) }

    fun deleteMedia(id: Long) = viewModelScope.launch { repo.deleteMedia(id) }

    // --- Yarış günlüğü ---

    fun addRaceNote(date: LocalDate, sim: String, track: String, car: String, bestLapMs: Long?, setupNotes: String) =
        viewModelScope.launch { repo.addRaceNote(date, sim, track, car, bestLapMs, setupNotes) }

    fun deleteRaceNote(id: Long) = viewModelScope.launch { repo.deleteRaceNote(id) }
}
