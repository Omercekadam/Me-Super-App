package com.omer.mesuper.feature.finance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Bütçenin kategori bilgisiyle birleştirilmiş görünümü (categoryId null = genel limit). */
data class BudgetRow(
    val id: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val emoji: String?,
    val monthlyLimitKurus: Long,
)

/** Hedef + katkı toplamı. */
data class GoalWithProgress(
    val id: Long,
    val name: String,
    val targetKurus: Long,
    val savedKurus: Long,
)

@Dao
interface PlanningDao {
    // --- Bütçeler ---
    @Query(
        """
        SELECT b.id, b.categoryId, c.name AS categoryName, c.emoji AS emoji, b.monthlyLimitKurus
        FROM budgets b LEFT JOIN categories c ON c.id = b.categoryId
        ORDER BY b.categoryId IS NOT NULL, c.name
        """
    )
    fun observeBudgets(): Flow<List<BudgetRow>>

    @Insert
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    // --- Abonelikler ---
    @Query("SELECT * FROM subscriptions ORDER BY billingDay")
    fun observeSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert
    suspend fun insertSubscription(sub: SubscriptionEntity): Long

    @Update
    suspend fun updateSubscription(sub: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscription(id: Long)

    // --- Hedefler ---
    @Query(
        """
        SELECT g.id, g.name, g.targetKurus, COALESCE(SUM(c.amountKurus), 0) AS savedKurus
        FROM goals g LEFT JOIN goal_contributions c ON c.goalId = g.id
        GROUP BY g.id
        ORDER BY g.id
        """
    )
    fun observeGoals(): Flow<List<GoalWithProgress>>

    @Insert
    suspend fun insertGoal(goal: GoalEntity): Long

    @Insert
    suspend fun insertContribution(contribution: GoalContributionEntity): Long

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    // --- Yedekleme (dump/restore) ---
    @Query("SELECT * FROM budgets")
    suspend fun dumpBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM subscriptions")
    suspend fun dumpSubscriptions(): List<SubscriptionEntity>

    @Query("SELECT * FROM goals")
    suspend fun dumpGoals(): List<GoalEntity>

    @Query("SELECT * FROM goal_contributions")
    suspend fun dumpContributions(): List<GoalContributionEntity>

    @Insert
    suspend fun insertBudgets(list: List<BudgetEntity>)

    @Insert
    suspend fun insertSubscriptions(list: List<SubscriptionEntity>)

    @Insert
    suspend fun insertGoals(list: List<GoalEntity>)

    @Insert
    suspend fun insertContributions(list: List<GoalContributionEntity>)

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    @Query("DELETE FROM subscriptions")
    suspend fun clearSubscriptions()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("DELETE FROM goal_contributions")
    suspend fun clearContributions()
}
