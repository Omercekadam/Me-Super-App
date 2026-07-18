package com.omer.mesuper.feature.finance.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val dao: FinanceDao,
) {
    val txRows: Flow<List<TxRow>> = dao.observeTxRows()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()

    suspend fun addTransaction(
        amountKurus: Long,
        type: TxType,
        categoryId: Long,
        occurredAt: LocalDate,
        note: String = "",
    ) {
        dao.insertTransaction(
            TransactionEntity(
                amountKurus = amountKurus,
                type = type,
                categoryId = categoryId,
                occurredAt = occurredAt,
                note = note,
            )
        )
    }
}
