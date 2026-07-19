package com.omer.mesuper.feature.agenda.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val createdAt: LocalDate = LocalDate.now(),
)

@Entity(
    tableName = "habit_ticks",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["habitId", "date"], unique = true)],
)
data class HabitTickEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: LocalDate,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: LocalDate? = null,
    val isDone: Boolean = false,
    val doneAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
)

@Entity(
    tableName = "pomodoro_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index("taskId"), Index("startedAt")],
)
data class PomodoroSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val startedAt: Instant,
    val durationMin: Int,
    val completed: Boolean,
)

/** GitHub GraphQL'den çekilen günlük commit önbelleği (streak Python'da hesaplanır). */
@Entity(tableName = "github_days")
data class GithubDayEntity(
    @PrimaryKey val date: LocalDate,
    val commitCount: Int,
)
