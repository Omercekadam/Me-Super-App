package com.omer.mesuper.feature.activity.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

enum class GameSource { STEAM, MANUAL }
enum class GameStatus { PLAYING, WISHLIST, DONE }

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: GameSource,
    val steamAppId: Long? = null,
    val rawgId: Long? = null,
    val name: String,
    val genres: String = "",
    val coverUrl: String? = null,
    val rating10: Int? = null,
    val status: GameStatus = GameStatus.PLAYING,
    val createdAt: Instant = Instant.now(),
)

/** Steam'in resmi API'sinden günlük toplam oynama süresi anlık görüntüsü — günlük fark Python'da hesaplanır. */
@Entity(
    tableName = "steam_snapshots",
    indices = [Index(value = ["steamAppId", "date"], unique = true)],
)
data class SteamSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val steamAppId: Long,
    val date: LocalDate,
    val playtimeForeverMin: Long,
)

/** Steam dışı platformlar (Epic, konsol vb.) için elle oynama süresi girişi. */
@Entity(
    tableName = "manual_play_logs",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("gameId"), Index("date")],
)
data class ManualPlayLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val date: LocalDate,
    val minutes: Int,
)

enum class MediaType { MOVIE, TV }
enum class MediaStatus { WATCHED, WATCHLIST }

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tmdbId: Long,
    val type: MediaType,
    val title: String,
    val posterUrl: String? = null,
    val rating10: Int? = null,
    val status: MediaStatus = MediaStatus.WATCHLIST,
    val watchedAt: LocalDate? = null,
)

@Entity(tableName = "race_notes")
data class RaceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val sim: String,
    val track: String,
    val car: String,
    val bestLapMs: Long? = null,
    val setupNotes: String = "",
)
