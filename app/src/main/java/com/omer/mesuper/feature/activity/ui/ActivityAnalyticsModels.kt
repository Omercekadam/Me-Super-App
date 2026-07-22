package com.omer.mesuper.feature.activity.ui

import kotlinx.serialization.Serializable

/** Python tarafındaki analytics.activity çıktılarının Kotlin karşılıkları. */

@Serializable
data class SteamDailyMinutes(val steamAppId: Long, val date: String, val minutesPlayed: Int)

@Serializable
data class GenreMinutes(val genre: String, val minutes: Int, val ratio: Double)

@Serializable
data class WeeklyBalance(val gameMinutes: Int, val workMinutes: Int, val gameRatio: Double, val workRatio: Double)
