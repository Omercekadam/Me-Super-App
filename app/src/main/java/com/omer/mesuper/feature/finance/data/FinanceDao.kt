package com.omer.mesuper.feature.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** İşlem satırının kategori bilgisiyle birleştirilmiş görünümü. */
data class TxRow(
    val id: Long,
    val amountKurus: Long,
    val type: TxType,
    val categoryName: String,
    val emoji: String,
    val occurredAt: LocalDate,
)

@Dao
interface FinanceDao {
    @Insert
    suspend fun insertTransaction(tx: TransactionEntity): Long

    @Query(
        """
        SELECT t.id, t.amountKurus, t.type, c.name AS categoryName, c.emoji AS emoji, t.occurredAt
        FROM transactions t JOIN categories c ON c.id = t.categoryId
        ORDER BY t.occurredAt DESC, t.id DESC
        """
    )
    fun observeTxRows(): Flow<List<TxRow>>

    @Query("SELECT * FROM categories")
    fun observeCategories(): Flow<List<CategoryEntity>>
}
