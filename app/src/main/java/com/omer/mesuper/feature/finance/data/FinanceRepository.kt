package com.omer.mesuper.feature.finance.data

import com.omer.mesuper.core.notifications.NotificationHelper
import com.omer.mesuper.core.ui.formatKurusAsTl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val dao: FinanceDao,
    private val planningDao: PlanningDao,
    private val notificationHelper: NotificationHelper,
) {
    val txRows: Flow<List<TxRow>> = dao.observeTxRows()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val budgets: Flow<List<BudgetRow>> = planningDao.observeBudgets()
    val subscriptions: Flow<List<SubscriptionEntity>> = planningDao.observeSubscriptions()
    val goals: Flow<List<GoalWithProgress>> = planningDao.observeGoals()

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
        if (type == TxType.EXPENSE) checkBudgetAlerts()
    }

    /** Bu ayın harcamalarını bütçe limitleriyle karşılaştırır; aşan limitler için bildirim gönderir. */
    private suspend fun checkBudgetAlerts() {
        val thisMonth = YearMonth.now()
        val monthRows = dao.observeTxRows().first()
            .filter { it.type == TxType.EXPENSE && YearMonth.from(it.occurredAt) == thisMonth }
        val totalExpense = monthRows.sumOf { it.amountKurus }
        val byCategory = monthRows.groupBy { it.categoryId }.mapValues { (_, rows) -> rows.sumOf { it.amountKurus } }

        planningDao.observeBudgets().first().forEach { budget ->
            val spent = if (budget.categoryId == null) totalExpense else byCategory[budget.categoryId] ?: 0L
            if (budget.monthlyLimitKurus > 0 && spent >= budget.monthlyLimitKurus) {
                val label = budget.categoryName ?: "Genel"
                notificationHelper.notifyBudgetExceeded(
                    budgetId = budget.id,
                    title = "Bütçe aşıldı: $label",
                    text = "${spent.formatKurusAsTl()} / ${budget.monthlyLimitKurus.formatKurusAsTl()} harcandı",
                )
            }
        }
    }

    suspend fun deleteTransaction(id: Long) = dao.deleteTransaction(id)

    suspend fun addBudget(categoryId: Long?, limitKurus: Long) =
        planningDao.insertBudget(BudgetEntity(categoryId = categoryId, monthlyLimitKurus = limitKurus))

    suspend fun deleteBudget(id: Long) = planningDao.deleteBudget(id)

    suspend fun addSubscription(sub: SubscriptionEntity) = planningDao.insertSubscription(sub)

    suspend fun deleteSubscription(id: Long) = planningDao.deleteSubscription(id)

    suspend fun addGoal(name: String, targetKurus: Long) =
        planningDao.insertGoal(GoalEntity(name = name, targetKurus = targetKurus))

    suspend fun addContribution(goalId: Long, amountKurus: Long, date: LocalDate) =
        planningDao.insertContribution(
            GoalContributionEntity(goalId = goalId, amountKurus = amountKurus, date = date)
        )

    suspend fun deleteGoal(id: Long) = planningDao.deleteGoal(id)
}
