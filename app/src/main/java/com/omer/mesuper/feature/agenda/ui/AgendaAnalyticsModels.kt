package com.omer.mesuper.feature.agenda.ui

import kotlinx.serialization.Serializable

/** Python tarafındaki analytics.agenda çıktılarının Kotlin karşılıkları. */

@Serializable
data class HabitStreak(
    val habitId: Long,
    val name: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val tickedToday: Boolean,
)

@Serializable
data class PomodoroStats(
    val todayMinutes: Int,
    val todaySessions: Int,
    val weekMinutes: Int,
    val weekSessions: Int,
    val totalMinutes: Int,
    val totalSessions: Int,
    val completionRate: Double,
)

@Serializable
data class GithubStreak(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalThisYear: Int,
)
