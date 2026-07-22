package com.omer.mesuper.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.omer.mesuper.feature.activity.data.ActivityDao
import com.omer.mesuper.feature.activity.data.GameEntity
import com.omer.mesuper.feature.activity.data.ManualPlayLogEntity
import com.omer.mesuper.feature.activity.data.MediaEntity
import com.omer.mesuper.feature.activity.data.RaceNoteEntity
import com.omer.mesuper.feature.activity.data.SteamSnapshotEntity
import com.omer.mesuper.feature.agenda.data.AgendaDao
import com.omer.mesuper.feature.agenda.data.GithubDayEntity
import com.omer.mesuper.feature.agenda.data.HabitEntity
import com.omer.mesuper.feature.agenda.data.HabitTickEntity
import com.omer.mesuper.feature.agenda.data.PomodoroSessionEntity
import com.omer.mesuper.feature.agenda.data.TaskEntity
import com.omer.mesuper.feature.finance.data.BudgetEntity
import com.omer.mesuper.feature.finance.data.CategoryEntity
import com.omer.mesuper.feature.finance.data.FinanceDao
import com.omer.mesuper.feature.finance.data.GoalContributionEntity
import com.omer.mesuper.feature.finance.data.GoalEntity
import com.omer.mesuper.feature.finance.data.PlanningDao
import com.omer.mesuper.feature.finance.data.SubscriptionEntity
import com.omer.mesuper.feature.finance.data.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SubscriptionEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        HabitEntity::class,
        HabitTickEntity::class,
        TaskEntity::class,
        PomodoroSessionEntity::class,
        GithubDayEntity::class,
        GameEntity::class,
        SteamSnapshotEntity::class,
        ManualPlayLogEntity::class,
        MediaEntity::class,
        RaceNoteEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun planningDao(): PlanningDao
    abstract fun agendaDao(): AgendaDao
    abstract fun activityDao(): ActivityDao

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
