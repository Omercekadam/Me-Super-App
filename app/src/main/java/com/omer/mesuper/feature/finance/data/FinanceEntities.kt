package com.omer.mesuper.feature.finance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

enum class TxType { INCOME, EXPENSE }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val type: TxType,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("categoryId"), Index("occurredAt")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Tutar her zaman kuruş cinsinden saklanır (100 kuruş = 1 TL). */
    val amountKurus: Long,
    val type: TxType,
    val categoryId: Long,
    val note: String = "",
    val occurredAt: LocalDate,
    val createdAt: Instant = Instant.now(),
)
