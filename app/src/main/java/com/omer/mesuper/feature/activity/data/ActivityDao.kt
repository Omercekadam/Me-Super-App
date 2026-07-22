package com.omer.mesuper.feature.activity.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    // --- Oyunlar ---
    @Insert
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGame(id: Long)

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun observeGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE steamAppId = :steamAppId LIMIT 1")
    suspend fun findGameBySteamAppId(steamAppId: Long): GameEntity?

    @Query("UPDATE games SET rating10 = :rating10 WHERE id = :id")
    suspend fun updateGameRating(id: Long, rating10: Int)

    @Query("UPDATE games SET status = :status WHERE id = :id")
    suspend fun updateGameStatus(id: Long, status: GameStatus)

    // --- Steam anlık görüntüleri ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSteamSnapshots(snapshots: List<SteamSnapshotEntity>)

    @Query("SELECT * FROM steam_snapshots ORDER BY date DESC")
    fun observeSteamSnapshots(): Flow<List<SteamSnapshotEntity>>

    // --- Elle oynama günlüğü ---
    @Insert
    suspend fun insertPlayLog(log: ManualPlayLogEntity): Long

    @Query("DELETE FROM manual_play_logs WHERE id = :id")
    suspend fun deletePlayLog(id: Long)

    @Query("SELECT * FROM manual_play_logs ORDER BY date DESC")
    fun observePlayLogs(): Flow<List<ManualPlayLogEntity>>

    // --- Film/Dizi ---
    @Insert
    suspend fun insertMedia(media: MediaEntity): Long

    @Update
    suspend fun updateMedia(media: MediaEntity)

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun deleteMedia(id: Long)

    @Query("SELECT * FROM media ORDER BY id DESC")
    fun observeMedia(): Flow<List<MediaEntity>>

    // --- Yarış günlüğü ---
    @Insert
    suspend fun insertRaceNote(note: RaceNoteEntity): Long

    @Query("DELETE FROM race_notes WHERE id = :id")
    suspend fun deleteRaceNote(id: Long)

    @Query("SELECT * FROM race_notes ORDER BY date DESC")
    fun observeRaceNotes(): Flow<List<RaceNoteEntity>>

    // --- Yedekleme (dump/restore) ---
    @Query("SELECT * FROM games")
    suspend fun dumpGames(): List<GameEntity>

    @Query("SELECT * FROM steam_snapshots")
    suspend fun dumpSteamSnapshots(): List<SteamSnapshotEntity>

    @Query("SELECT * FROM manual_play_logs")
    suspend fun dumpPlayLogs(): List<ManualPlayLogEntity>

    @Query("SELECT * FROM media")
    suspend fun dumpMedia(): List<MediaEntity>

    @Query("SELECT * FROM race_notes")
    suspend fun dumpRaceNotes(): List<RaceNoteEntity>

    @Insert
    suspend fun insertGames(list: List<GameEntity>)

    @Insert
    suspend fun insertSteamSnapshots(list: List<SteamSnapshotEntity>)

    @Insert
    suspend fun insertPlayLogs(list: List<ManualPlayLogEntity>)

    @Insert
    suspend fun insertMediaList(list: List<MediaEntity>)

    @Insert
    suspend fun insertRaceNotes(list: List<RaceNoteEntity>)

    @Query("DELETE FROM games")
    suspend fun clearGames()

    @Query("DELETE FROM steam_snapshots")
    suspend fun clearSteamSnapshots()

    @Query("DELETE FROM manual_play_logs")
    suspend fun clearPlayLogs()

    @Query("DELETE FROM media")
    suspend fun clearMedia()

    @Query("DELETE FROM race_notes")
    suspend fun clearRaceNotes()
}
