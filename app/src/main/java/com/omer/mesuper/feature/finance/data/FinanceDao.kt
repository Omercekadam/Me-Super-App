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
    val categoryId: Long,
    val categoryName: String,
    val emoji: String,
    val colorHex: String,
    val note: String,
    val occurredAt: LocalDate,
)

@Dao
interface FinanceDao {
    @Insert
    suspend fun insertTransaction(tx: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query(
        """
        SELECT t.id, t.amountKurus, t.type, t.categoryId, c.name AS categoryName,
               c.emoji AS emoji, c.colorHex AS colorHex, t.note, t.occurredAt
        FROM transactions t JOIN categories c ON c.id = t.categoryId
        ORDER BY t.occurredAt DESC, t.id DESC
        """
    )
    fun observeTxRows(): Flow<List<TxRow>>

    @Query("SELECT * FROM categories")
    fun observeCategories(): Flow<List<CategoryEntity>>

    // --- Yedekleme (dump/restore) ---
    @Query("SELECT * FROM transactions")
    suspend fun dumpTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM categories")
    suspend fun dumpCategories(): List<CategoryEntity>

    @Insert
    suspend fun insertTransactions(list: List<TransactionEntity>)

    @Insert
    suspend fun insertCategories(list: List<CategoryEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()
}
