package com.omer.mesuper.core.backup

import androidx.room.withTransaction
import com.omer.mesuper.core.database.AppDatabase
import com.omer.mesuper.feature.finance.data.BudgetEntity
import com.omer.mesuper.feature.finance.data.CategoryEntity
import com.omer.mesuper.feature.finance.data.FinanceDao
import com.omer.mesuper.feature.finance.data.GoalContributionEntity
import com.omer.mesuper.feature.finance.data.GoalEntity
import com.omer.mesuper.feature.finance.data.PlanningDao
import com.omer.mesuper.feature.finance.data.SubscriptionEntity
import com.omer.mesuper.feature.finance.data.SubscriptionPeriod
import com.omer.mesuper.feature.finance.data.TransactionEntity
import com.omer.mesuper.feature.finance.data.TxType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Tüm veritabanının taşınabilir JSON temsili. Şema değişirse version artırılır. */
@Serializable
data class BackupDto(
    val version: Int = 1,
    val exportedAt: String,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
    val budgets: List<BudgetDto>,
    val subscriptions: List<SubscriptionDto>,
    val goals: List<GoalDto>,
    val goalContributions: List<ContributionDto>,
)

@Serializable
data class CategoryDto(val id: Long, val name: String, val emoji: String, val colorHex: String, val type: String)

@Serializable
data class TransactionDto(
    val id: Long, val amountKurus: Long, val type: String, val categoryId: Long,
    val note: String, val occurredAt: String, val createdAtMs: Long,
)

@Serializable
data class BudgetDto(val id: Long, val categoryId: Long?, val monthlyLimitKurus: Long)

@Serializable
data class SubscriptionDto(
    val id: Long, val name: String, val amountKurus: Long, val billingDay: Int,
    val period: String, val billingMonth: Int?, val categoryId: Long?, val isActive: Boolean,
)

@Serializable
data class GoalDto(val id: Long, val name: String, val targetKurus: Long)

@Serializable
data class ContributionDto(val id: Long, val goalId: Long, val amountKurus: Long, val date: String)

@Singleton
class BackupManager @Inject constructor(
    private val db: AppDatabase,
    private val financeDao: FinanceDao,
    private val planningDao: PlanningDao,
) {
    // encodeDefaults: version alanı her yedekte açıkça yazılsın (ileri uyumluluk)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun exportJson(): String {
        val dto = BackupDto(
            exportedAt = Instant.now().toString(),
            categories = financeDao.dumpCategories().map {
                CategoryDto(it.id, it.name, it.emoji, it.colorHex, it.type.name)
            },
            transactions = financeDao.dumpTransactions().map {
                TransactionDto(
                    it.id, it.amountKurus, it.type.name, it.categoryId,
                    it.note, it.occurredAt.toString(), it.createdAt.toEpochMilli(),
                )
            },
            budgets = planningDao.dumpBudgets().map {
                BudgetDto(it.id, it.categoryId, it.monthlyLimitKurus)
            },
            subscriptions = planningDao.dumpSubscriptions().map {
                SubscriptionDto(
                    it.id, it.name, it.amountKurus, it.billingDay,
                    it.period.name, it.billingMonth, it.categoryId, it.isActive,
                )
            },
            goals = planningDao.dumpGoals().map { GoalDto(it.id, it.name, it.targetKurus) },
            goalContributions = planningDao.dumpContributions().map {
                ContributionDto(it.id, it.goalId, it.amountKurus, it.date.toString())
            },
        )
        return json.encodeToString(BackupDto.serializer(), dto)
    }

    /** Mevcut TÜM veriyi siler ve yedekten geri yükler (tek transaction). */
    suspend fun importJson(raw: String) {
        val dto = json.decodeFromString(BackupDto.serializer(), raw)
        require(dto.version == 1) { "Desteklenmeyen yedek sürümü: ${dto.version}" }
        db.withTransaction {
            planningDao.clearContributions()
            planningDao.clearGoals()
            planningDao.clearSubscriptions()
            planningDao.clearBudgets()
            financeDao.clearTransactions()
            financeDao.clearCategories()

            financeDao.insertCategories(dto.categories.map {
                CategoryEntity(it.id, it.name, it.emoji, it.colorHex, TxType.valueOf(it.type))
            })
            financeDao.insertTransactions(dto.transactions.map {
                TransactionEntity(
                    id = it.id,
                    amountKurus = it.amountKurus,
                    type = TxType.valueOf(it.type),
                    categoryId = it.categoryId,
                    note = it.note,
                    occurredAt = LocalDate.parse(it.occurredAt),
                    createdAt = Instant.ofEpochMilli(it.createdAtMs),
                )
            })
            planningDao.insertBudgets(dto.budgets.map {
                BudgetEntity(it.id, it.categoryId, it.monthlyLimitKurus)
            })
            planningDao.insertSubscriptions(dto.subscriptions.map {
                SubscriptionEntity(
                    id = it.id, name = it.name, amountKurus = it.amountKurus,
                    billingDay = it.billingDay, period = SubscriptionPeriod.valueOf(it.period),
                    billingMonth = it.billingMonth, categoryId = it.categoryId, isActive = it.isActive,
                )
            })
            planningDao.insertGoals(dto.goals.map { GoalEntity(it.id, it.name, it.targetKurus) })
            planningDao.insertContributions(dto.goalContributions.map {
                GoalContributionEntity(it.id, it.goalId, it.amountKurus, LocalDate.parse(it.date))
            })
        }
    }
}
