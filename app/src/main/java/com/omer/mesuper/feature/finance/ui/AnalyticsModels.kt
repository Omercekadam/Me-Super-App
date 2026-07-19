package com.omer.mesuper.feature.finance.ui

import kotlinx.serialization.Serializable

/** Python tarafındaki analytics.finance çıktılarının Kotlin karşılıkları. */

@Serializable
data class CategorySum(val category: String, val totalKurus: Long, val share: Double)

@Serializable
data class FinanceSummary(
    val month: String,
    val incomeKurus: Long,
    val expenseKurus: Long,
    val balanceKurus: Long,
    val byCategory: List<CategorySum> = emptyList(),
    val engine: String = "",
)

@Serializable
data class Forecast(
    val month: String,
    val spentKurus: Long,
    val dailyPaceKurus: Long,
    val remainingSubsKurus: Long,
    val daysLeft: Int,
    val projectedKurus: Long,
)

@Serializable
data class Insight(val severity: String, val text: String)
