package com.omer.mesuper.feature.activity.data

import com.omer.mesuper.core.datastore.UserPrefsStore
import com.omer.mesuper.core.network.RawgApi
import com.omer.mesuper.core.network.RawgGameResult
import com.omer.mesuper.core.network.SteamApi
import com.omer.mesuper.core.network.TmdbApi
import com.omer.mesuper.core.network.TmdbMediaKind
import com.omer.mesuper.core.network.TmdbSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val dao: ActivityDao,
    private val steamApi: SteamApi,
    private val rawgApi: RawgApi,
    private val tmdbApi: TmdbApi,
    private val userPrefsStore: UserPrefsStore,
) {
    val games: Flow<List<GameEntity>> = dao.observeGames()
    val steamSnapshots: Flow<List<SteamSnapshotEntity>> = dao.observeSteamSnapshots()
    val playLogs: Flow<List<ManualPlayLogEntity>> = dao.observePlayLogs()
    val media: Flow<List<MediaEntity>> = dao.observeMedia()
    val raceNotes: Flow<List<RaceNoteEntity>> = dao.observeRaceNotes()

    suspend fun addManualGame(name: String, genres: String, status: GameStatus): Long =
        dao.insertGame(GameEntity(source = GameSource.MANUAL, name = name, genres = genres, status = status))

    suspend fun addRawgGame(rawgId: Long, name: String, genres: String, coverUrl: String?, status: GameStatus): Long =
        dao.insertGame(
            GameEntity(
                source = GameSource.MANUAL,
                rawgId = rawgId,
                name = name,
                genres = genres,
                coverUrl = coverUrl,
                status = status,
            )
        )

    suspend fun rateGame(gameId: Long, rating10: Int) = dao.updateGameRating(gameId, rating10)

    suspend fun setGameStatus(gameId: Long, status: GameStatus) = dao.updateGameStatus(gameId, status)

    suspend fun deleteGame(id: Long) = dao.deleteGame(id)

    suspend fun addPlayLog(gameId: Long, date: LocalDate, minutes: Int) =
        dao.insertPlayLog(ManualPlayLogEntity(gameId = gameId, date = date, minutes = minutes))

    suspend fun deletePlayLog(id: Long) = dao.deletePlayLog(id)

    suspend fun upsertSteamSnapshots(snapshots: List<SteamSnapshotEntity>) = dao.upsertSteamSnapshots(snapshots)

    suspend fun addMedia(tmdbId: Long, type: MediaType, title: String, posterUrl: String?): Long =
        dao.insertMedia(MediaEntity(tmdbId = tmdbId, type = type, title = title, posterUrl = posterUrl))

    suspend fun markMediaWatched(media: MediaEntity, rating10: Int?, watchedAt: LocalDate = LocalDate.now()) =
        dao.updateMedia(media.copy(status = MediaStatus.WATCHED, rating10 = rating10, watchedAt = watchedAt))

    suspend fun deleteMedia(id: Long) = dao.deleteMedia(id)

    suspend fun addRaceNote(date: LocalDate, sim: String, track: String, car: String, bestLapMs: Long?, setupNotes: String) =
        dao.insertRaceNote(
            RaceNoteEntity(date = date, sim = sim, track = track, car = car, bestLapMs = bestLapMs, setupNotes = setupNotes)
        )

    suspend fun deleteRaceNote(id: Long) = dao.deleteRaceNote(id)

    /** Ayarlarda kayıtlı Steam API anahtarı + Steam ID ile kütüphaneyi çeker; her oyun için bugünün anlık görüntüsünü kaydeder. */
    suspend fun syncSteamLibrary() {
        val apiKey = userPrefsStore.steamApiKey.first()
        val steamId = userPrefsStore.steamId.first()
        require(!apiKey.isNullOrBlank() && !steamId.isNullOrBlank()) {
            "Steam API anahtarı ve Steam ID Ayarlar'dan girilmeli"
        }
        val ownedGames = steamApi.fetchOwnedGames(apiKey, steamId)
        val today = LocalDate.now()
        val snapshots = ownedGames.map { game ->
            val existing = dao.findGameBySteamAppId(game.appId)
            if (existing == null) {
                dao.insertGame(
                    GameEntity(source = GameSource.STEAM, steamAppId = game.appId, name = game.name, status = GameStatus.PLAYING)
                )
            } else if (existing.name != game.name) {
                dao.updateGame(existing.copy(name = game.name))
            }
            SteamSnapshotEntity(steamAppId = game.appId, date = today, playtimeForeverMin = game.playtimeForeverMin)
        }
        dao.upsertSteamSnapshots(snapshots)
    }

    /** Ayarlarda kayıtlı RAWG anahtarıyla oyun arar (elle ekleme için kapak/tür bilgisi). */
    suspend fun searchRawgGames(query: String): List<RawgGameResult> {
        val apiKey = userPrefsStore.rawgApiKey.first()
        require(!apiKey.isNullOrBlank()) { "RAWG API anahtarı Ayarlar'dan girilmeli" }
        return rawgApi.searchGames(apiKey, query)
    }

    /** Ayarlarda kayıtlı TMDB anahtarıyla film/dizi arar. */
    suspend fun searchTmdb(kind: TmdbMediaKind, query: String): List<TmdbSearchResult> {
        val apiKey = userPrefsStore.tmdbApiKey.first()
        require(!apiKey.isNullOrBlank()) { "TMDB API anahtarı Ayarlar'dan girilmeli" }
        return tmdbApi.search(apiKey, kind, query)
    }
}
