package com.omer.mesuper.feature.finance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class SubscriptionPeriod { MONTHLY, YEARLY }

/** Aylık harcama limiti. categoryId null ise genel (tüm giderler) limiti. */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val monthlyLimitKurus: Long,
)

@Entity(
    tableName = "subscriptions",
    indices = [Index("categoryId")],
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountKurus: Long,
    /** Ayın kaçında yenilenir (1-28 arası önerilir). */
    val billingDay: Int,
    val period: SubscriptionPeriod = SubscriptionPeriod.MONTHLY,
    /** YEARLY aboneliklerde yenilenme ayı (1-12); MONTHLY için yok sayılır. */
    val billingMonth: Int? = null,
    val categoryId: Long? = null,
    val isActive: Boolean = true,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetKurus: Long,
)

@Entity(
    tableName = "goal_contributions",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("goalId")],
)
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amountKurus: Long,
    val date: LocalDate,
)
