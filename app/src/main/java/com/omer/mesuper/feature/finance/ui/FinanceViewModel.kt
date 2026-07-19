package com.omer.mesuper.feature.finance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omer.mesuper.core.python.AnalyticsEngine
import com.omer.mesuper.feature.finance.data.BudgetRow
import com.omer.mesuper.feature.finance.data.CategoryEntity
import com.omer.mesuper.feature.finance.data.FinanceRepository
import com.omer.mesuper.feature.finance.data.GoalWithProgress
import com.omer.mesuper.feature.finance.data.SubscriptionEntity
import com.omer.mesuper.feature.finance.data.SubscriptionPeriod
import com.omer.mesuper.feature.finance.data.TxRow
import com.omer.mesuper.feature.finance.data.TxType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Bütçe satırı + o ayki gerçekleşen harcama. */
data class BudgetUi(
    val row: BudgetRow,
    val spentKurus: Long,
) {
    val ratio: Float
        get() = if (row.monthlyLimitKurus > 0) {
            (spentKurus.toFloat() / row.monthlyLimitKurus).coerceAtMost(1.5f)
        } else 0f
}

data class FinanceUiState(
    val month: YearMonth = YearMonth.now(),
    val summary: FinanceSummary? = null,
    val forecast: Forecast? = null,
    val insights: List<Insight> = emptyList(),
    val budgets: List<BudgetUi> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val monthlyFixedKurus: Long = 0,
    val goals: List<GoalWithProgress> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TxRow> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

private data class RawData(
    val txns: List<TxRow>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetRow>,
    val subscriptions: List<SubscriptionEntity>,
    val goals: List<GoalWithProgress>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repo: FinanceRepository,
    private val engine: AnalyticsEngine,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val uiState: StateFlow<FinanceUiState> = combine(
        repo.txRows, repo.categories, repo.budgets, repo.subscriptions, repo.goals,
    ) { txns, categories, budgets, subscriptions, goals ->
        RawData(txns, categories, budgets, subscriptions, goals)
    }
        .mapLatest(::buildState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState())

    private suspend fun buildState(raw: RawData): FinanceUiState {
        val month = YearMonth.now()
        val prevMonth = month.minusMonths(1)
        val today = LocalDate.now()

        val curRows = raw.txns.filter { YearMonth.from(it.occurredAt) == month }
        val bothMonthsRows = raw.txns.filter {
            val ym = YearMonth.from(it.occurredAt)
            ym == month || ym == prevMonth
        }

        return runCatching {
            val summary: FinanceSummary = json.decodeFromString(
                engine.run("finance.summary", buildJsonObject {
                    put("month", month.toString())
                    put("txns", txnsJson(curRows))
                }.toString())
            )
            val forecast: Forecast = json.decodeFromString(
                engine.run("finance.forecast", buildJsonObject {
                    put("month", month.toString())
                    put("today", today.toString())
                    put("daysInMonth", month.lengthOfMonth())
                    put("txns", txnsJson(curRows))
                    put("subscriptions", subsJson(raw.subscriptions))
                }.toString())
            )
            val insights: List<Insight> = json.decodeFromString(
                engine.run("finance.insights", buildJsonObject {
                    put("month", month.toString())
                    put("prevMonth", prevMonth.toString())
                    put("txns", txnsJson(bothMonthsRows))
                    putJsonArray("budgets") {
                        raw.budgets.forEach { b ->
                            addJsonObject {
                                if (b.categoryName == null) {
                                    put("categoryName", null as String?)
                                } else {
                                    put("categoryName", b.categoryName)
                                }
                                put("monthlyLimitKurus", b.monthlyLimitKurus)
                            }
                        }
                    }
                }.toString())
            )

            val expenseByCategory = curRows
                .filter { it.type == TxType.EXPENSE }
                .groupBy { it.categoryId }
                .mapValues { (_, rows) -> rows.sumOf { it.amountKurus } }
            val totalExpense = expenseByCategory.values.sum()

            FinanceUiState(
                month = month,
                summary = summary,
                forecast = forecast,
                insights = insights,
                budgets = raw.budgets.map { b ->
                    BudgetUi(
                        row = b,
                        spentKurus = if (b.categoryId == null) totalExpense
                        else expenseByCategory[b.categoryId] ?: 0L,
                    )
                },
                subscriptions = raw.subscriptions,
                monthlyFixedKurus = raw.subscriptions
                    .filter { it.isActive }
                    .sumOf {
                        when (it.period) {
                            SubscriptionPeriod.MONTHLY -> it.amountKurus
                            SubscriptionPeriod.YEARLY -> it.amountKurus / 12
                        }
                    },
                goals = raw.goals,
                categories = raw.categories,
                transactions = raw.txns,
                loading = false,
            )
        }.getOrElse { e ->
            FinanceUiState(
                month = month,
                categories = raw.categories,
                transactions = raw.txns,
                loading = false,
                error = e.message ?: e.toString(),
            )
        }
    }

    private fun txnsJson(rows: List<TxRow>): JsonElement = buildJsonArray {
        rows.forEach { r ->
            addJsonObject {
                put("amountKurus", r.amountKurus)
                put("type", r.type.name)
                put("category", r.categoryName)
                put("occurredAt", r.occurredAt.toString())
            }
        }
    }

    private fun subsJson(subs: List<SubscriptionEntity>): JsonElement = buildJsonArray {
        subs.forEach { s ->
            addJsonObject {
                put("amountKurus", s.amountKurus)
                put("billingDay", s.billingDay)
                put("period", s.period.name)
                put("billingMonth", s.billingMonth)
                put("isActive", s.isActive)
            }
        }
    }

    // --- Kullanıcı eylemleri ---

    fun deleteTransaction(id: Long) = viewModelScope.launch { repo.deleteTransaction(id) }

    fun addBudget(categoryId: Long?, limitKurus: Long) =
        viewModelScope.launch { repo.addBudget(categoryId, limitKurus) }

    fun deleteBudget(id: Long) = viewModelScope.launch { repo.deleteBudget(id) }

    fun addSubscription(
        name: String,
        amountKurus: Long,
        billingDay: Int,
        period: SubscriptionPeriod,
        billingMonth: Int?,
    ) = viewModelScope.launch {
        repo.addSubscription(
            SubscriptionEntity(
                name = name,
                amountKurus = amountKurus,
                billingDay = billingDay.coerceIn(1, 28),
                period = period,
                billingMonth = billingMonth,
            )
        )
    }

    fun deleteSubscription(id: Long) = viewModelScope.launch { repo.deleteSubscription(id) }

    fun addGoal(name: String, targetKurus: Long) =
        viewModelScope.launch { repo.addGoal(name, targetKurus) }

    fun contribute(goalId: Long, amountKurus: Long) =
        viewModelScope.launch { repo.addContribution(goalId, amountKurus, LocalDate.now()) }

    fun deleteGoal(id: Long) = viewModelScope.launch { repo.deleteGoal(id) }
}
