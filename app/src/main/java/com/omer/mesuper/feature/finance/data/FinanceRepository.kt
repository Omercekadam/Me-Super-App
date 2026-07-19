package com.omer.mesuper.feature.finance.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val dao: FinanceDao,
    private val planningDao: PlanningDao,
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
