package com.omer.mesuper.feature.agenda.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AgendaDao {
    // --- Alışkanlıklar ---
    @Insert
    suspend fun insertHabit(habit: HabitEntity): Long

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("SELECT * FROM habits ORDER BY id")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tickHabit(tick: HabitTickEntity)

    @Query("DELETE FROM habit_ticks WHERE habitId = :habitId AND date = :date")
    suspend fun untickHabit(habitId: Long, date: LocalDate)

    @Query("SELECT * FROM habit_ticks ORDER BY date DESC")
    fun observeTicks(): Flow<List<HabitTickEntity>>

    // --- Yapılacaklar ---
    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM tasks ORDER BY isDone, dueDate IS NULL, dueDate, id DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    // --- Pomodoro ---
    @Insert
    suspend fun insertSession(session: PomodoroSessionEntity): Long

    @Query("SELECT * FROM pomodoro_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<PomodoroSessionEntity>>

    // --- GitHub önbelleği ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGithubDays(days: List<GithubDayEntity>)

    @Query("SELECT * FROM github_days ORDER BY date DESC")
    fun observeGithubDays(): Flow<List<GithubDayEntity>>

    @Query("DELETE FROM github_days")
    suspend fun clearGithubDays()

    // --- Yedekleme (dump/restore) ---
    @Query("SELECT * FROM habits")
    suspend fun dumpHabits(): List<HabitEntity>

    @Query("SELECT * FROM habit_ticks")
    suspend fun dumpTicks(): List<HabitTickEntity>

    @Query("SELECT * FROM tasks")
    suspend fun dumpTasks(): List<TaskEntity>

    @Query("SELECT * FROM pomodoro_sessions")
    suspend fun dumpSessions(): List<PomodoroSessionEntity>

    @Query("SELECT * FROM github_days")
    suspend fun dumpGithubDays(): List<GithubDayEntity>

    @Insert
    suspend fun insertHabits(list: List<HabitEntity>)

    @Insert
    suspend fun insertTicks(list: List<HabitTickEntity>)

    @Insert
    suspend fun insertTasks(list: List<TaskEntity>)

    @Insert
    suspend fun insertSessions(list: List<PomodoroSessionEntity>)

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("DELETE FROM habit_ticks")
    suspend fun clearTicks()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM pomodoro_sessions")
    suspend fun clearSessions()
}
