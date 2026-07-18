package com.omer.mesuper.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omer.mesuper.core.python.AnalyticsEngine
import com.omer.mesuper.feature.finance.data.FinanceRepository
import com.omer.mesuper.feature.finance.data.TxRow
import com.omer.mesuper.feature.finance.data.TxType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

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

data class ProofUiState(
    val rows: List<TxRow> = emptyList(),
    val summary: FinanceSummary? = null,
    val error: String? = null,
)

@HiltViewModel
class ProofViewModel @Inject constructor(
    private val repo: FinanceRepository,
    private val engine: AnalyticsEngine,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val uiState: StateFlow<ProofUiState> = repo.txRows
        .map { rows ->
            runCatching { ProofUiState(rows = rows, summary = summarize(rows)) }
                .getOrElse { ProofUiState(rows = rows, error = it.message ?: it.toString()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProofUiState())

    private suspend fun summarize(rows: List<TxRow>): FinanceSummary {
        val payload = buildJsonObject {
            put("month", YearMonth.now().toString())
            putJsonArray("txns") {
                rows.forEach { r ->
                    addJsonObject {
                        put("amountKurus", r.amountKurus)
                        put("type", r.type.name)
                        put("category", r.categoryName)
                        put("occurredAt", r.occurredAt.toString())
                    }
                }
            }
        }
        return json.decodeFromString(engine.run("finance.summary", payload.toString()))
    }

    fun addRandomExpense() = addRandom(TxType.EXPENSE)
    fun addRandomIncome() = addRandom(TxType.INCOME)

    private fun addRandom(type: TxType) {
        viewModelScope.launch {
            val categories = repo.categories.first().filter { it.type == type }
            if (categories.isEmpty()) return@launch
            val today = LocalDate.now()
            val amount = when (type) {
                TxType.EXPENSE -> (2_000L..60_000L).random()
                TxType.INCOME -> (100_000L..500_000L).random()
            }
            repo.addTransaction(
                amountKurus = amount,
                type = type,
                categoryId = categories.random().id,
                occurredAt = today.withDayOfMonth((1..today.dayOfMonth).random()),
            )
        }
    }
}
