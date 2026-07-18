package com.omer.mesuper.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.omer.mesuper.feature.finance.data.CategoryEntity
import com.omer.mesuper.feature.finance.data.FinanceDao
import com.omer.mesuper.feature.finance.data.TransactionEntity

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        /** İlk kurulumda tohum kategorileri ekler. */
        val SEED_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val rows = listOf(
                    Triple("Yemek", "🍔", "#EF6C00") to "EXPENSE",
                    Triple("Market", "🛒", "#388E3C") to "EXPENSE",
                    Triple("Oyun", "🎮", "#7B1FA2") to "EXPENSE",
                    Triple("Ulaşım", "🚌", "#1976D2") to "EXPENSE",
                    Triple("Abonelik", "📦", "#455A64") to "EXPENSE",
                    Triple("Eğlence", "🎬", "#C2185B") to "EXPENSE",
                    Triple("Diğer", "📌", "#616161") to "EXPENSE",
                    Triple("Maaş", "💰", "#2E7D32") to "INCOME",
                    Triple("Ek Gelir", "💸", "#00796B") to "INCOME",
                )
                rows.forEach { (info, type) ->
                    val (name, emoji, color) = info
                    db.execSQL(
                        "INSERT INTO categories (name, emoji, colorHex, type) VALUES (?, ?, ?, ?)",
                        arrayOf(name, emoji, color, type),
                    )
                }
            }
        }
    }
}
