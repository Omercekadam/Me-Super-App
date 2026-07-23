package com.omer.mesuper.feature.finance.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omer.mesuper.core.ui.DeleteButton
import com.omer.mesuper.core.ui.HeroPanel
import com.omer.mesuper.core.ui.LocalModuleColors
import com.omer.mesuper.core.ui.RowDivider
import com.omer.mesuper.core.ui.ScreenTitle
import com.omer.mesuper.core.ui.SectionHeader
import com.omer.mesuper.core.ui.SoftPanel
import com.omer.mesuper.core.ui.formatKurusAsTl
import com.omer.mesuper.feature.finance.data.GoalWithProgress
import com.omer.mesuper.feature.finance.data.TxRow
import com.omer.mesuper.feature.finance.data.TxType
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie
import java.time.format.DateTimeFormatter
import java.util.Locale

private val txDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("tr", "TR"))

private fun parseHex(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

@Composable
fun FinanceScreen(vm: FinanceViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }
    var showSubDialog by rememberSaveable { mutableStateOf(false) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var contributeGoalId by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item { ScreenTitle(title = "Finans", subtitle = "Bu ay • ${state.month}") }

        state.error?.let { error ->
            item {
                SoftPanel(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text("Analiz hatası: $error")
                }
            }
        }

        item { SummaryHero(state) }

        if (state.insights.isNotEmpty()) {
            item {
                SoftPanel(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    state.insights.forEach { insight ->
                        val emoji = when (insight.severity) {
                            "alert" -> "🚨"; "warn" -> "⚠️"; else -> "💡"
                        }
                        Text("$emoji ${insight.text}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (!state.summary?.byCategory.isNullOrEmpty()) {
            item { PieSection(state) }
        }
        state.forecast?.let { item { ForecastSection(state) } }

        item {
            BudgetsSection(
                state = state,
                onAdd = { showBudgetDialog = true },
                onDelete = vm::deleteBudget,
            )
        }
        item {
            SubscriptionsSection(
                state = state,
                onAdd = { showSubDialog = true },
                onDelete = vm::deleteSubscription,
            )
        }
        item {
            GoalsSection(
                goals = state.goals,
                onAdd = { showGoalDialog = true },
                onContribute = { contributeGoalId = it },
                onDelete = vm::deleteGoal,
            )
        }

        if (state.transactions.isNotEmpty()) {
            item {
                val shown = state.transactions.take(50)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(
                        "Son İşlemler",
                        icon = Icons.Default.Payments,
                        module = LocalModuleColors.current.finance,
                    )
                    shown.forEachIndexed { index, row ->
                        TransactionRow(row, onDelete = { vm.deleteTransaction(row.id) })
                        if (index < shown.lastIndex) RowDivider()
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        AddBudgetDialog(
            categories = state.categories,
            onConfirm = vm::addBudget,
            onDismiss = { showBudgetDialog = false },
        )
    }
    if (showSubDialog) {
        AddSubscriptionDialog(
            onConfirm = vm::addSubscription,
            onDismiss = { showSubDialog = false },
        )
    }
    if (showGoalDialog) {
        AddGoalDialog(onConfirm = vm::addGoal, onDismiss = { showGoalDialog = false })
    }
    contributeGoalId?.let { goalId ->
        val goal = state.goals.firstOrNull { it.id == goalId }
        if (goal != null) {
            ContributeDialog(
                goalName = goal.name,
                onConfirm = { vm.contribute(goalId, it) },
                onDismiss = { contributeGoalId = null },
            )
        }
    }
}

/** Odak çıpası: bakiye + gelir/gider (solid hero, gradyansız). */
@Composable
private fun SummaryHero(state: FinanceUiState) {
    HeroPanel(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Bakiye", style = MaterialTheme.typography.titleMedium)
        val summary = state.summary
        if (summary == null) {
            Text(if (state.loading) "Yükleniyor…" else "Henüz veri yok")
        } else {
            Text(summary.balanceKurus.formatKurusAsTl(), style = MaterialTheme.typography.displaySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gelir: ${summary.incomeKurus.formatKurusAsTl()}", style = MaterialTheme.typography.bodyMedium)
                Text("Gider: ${summary.expenseKurus.formatKurusAsTl()}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PieSection(state: FinanceUiState) {
    val byCategory = state.summary?.byCategory.orEmpty()
    val colorByName = state.categories.associate { it.name to parseHex(it.colorHex) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Harcama Dağılımı", icon = Icons.Default.PieChart, module = LocalModuleColors.current.finance)
        Row(verticalAlignment = Alignment.CenterVertically) {
            PieChart(
                modifier = Modifier.size(140.dp),
                data = byCategory.map { cat ->
                    Pie(
                        data = cat.totalKurus.toDouble(),
                        color = colorByName[cat.category] ?: Color.Gray,
                    )
                },
                style = Pie.Style.Stroke(width = 26.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                byCategory.forEach { cat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(colorByName[cat.category] ?: Color.Gray, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${cat.category} %${"%.0f".format(cat.share * 100)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastSection(state: FinanceUiState) {
    val forecast = state.forecast ?: return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("Ay Sonu Tahmini", icon = Icons.Default.TrendingUp, module = LocalModuleColors.current.finance)
        Text(forecast.projectedKurus.formatKurusAsTl(), style = MaterialTheme.typography.headlineSmall)
        Text(
            "Günlük ort. ${forecast.dailyPaceKurus.formatKurusAsTl()} • " +
                "kalan ${forecast.daysLeft} gün • " +
                "bekleyen abonelik ${forecast.remainingSubsKurus.formatKurusAsTl()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BudgetsSection(
    state: FinanceUiState,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            "Harcama Limitleri",
            icon = Icons.Default.Speed,
            module = LocalModuleColors.current.finance,
            trailing = { TextButton(onClick = onAdd) { Text("+ Ekle") } },
        )
        if (state.budgets.isEmpty()) {
            Text("Limit yok. Genel veya kategori bazlı limit ekle.", style = MaterialTheme.typography.bodySmall)
        }
        state.budgets.forEach { budget ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val label = budget.row.categoryName?.let { "${budget.row.emoji} $it" } ?: "🧮 Genel"
                    Text(label)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${budget.spentKurus.formatKurusAsTl()} / ${budget.row.monthlyLimitKurus.formatKurusAsTl()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        DeleteButton { onDelete(budget.row.id) }
                    }
                }
                val animatedRatio by animateFloatAsState(
                    targetValue = budget.ratio.coerceAtMost(1f),
                    label = "budgetRatio",
                )
                LinearProgressIndicator(
                    progress = { animatedRatio },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        budget.ratio >= 1f -> MaterialTheme.colorScheme.error
                        budget.ratio >= 0.85f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}

@Composable
private fun SubscriptionsSection(
    state: FinanceUiState,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            "Abonelikler",
            icon = Icons.Default.Autorenew,
            module = LocalModuleColors.current.finance,
            trailing = { TextButton(onClick = onAdd) { Text("+ Ekle") } },
        )
        if (state.subscriptions.isEmpty()) {
            Text("Abonelik yok.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                "Aylık sabit gider: ${state.monthlyFixedKurus.formatKurusAsTl()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        state.subscriptions.forEachIndexed { index, sub ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("📦 ${sub.name}")
                    Text(
                        if (sub.period.name == "MONTHLY") "Her ayın ${sub.billingDay}. günü"
                        else "Yılda bir: ${sub.billingMonth ?: 1}. ayın ${sub.billingDay}. günü",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sub.amountKurus.formatKurusAsTl())
                    DeleteButton { onDelete(sub.id) }
                }
            }
            if (index < state.subscriptions.lastIndex) RowDivider()
        }
    }
}

@Composable
private fun GoalsSection(
    goals: List<GoalWithProgress>,
    onAdd: () -> Unit,
    onContribute: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            "Birikim Hedefleri",
            icon = Icons.Default.Savings,
            module = LocalModuleColors.current.finance,
            trailing = { TextButton(onClick = onAdd) { Text("+ Ekle") } },
        )
        if (goals.isEmpty()) {
            Text("Hedef yok.", style = MaterialTheme.typography.bodySmall)
        }
        goals.forEach { goal ->
            val ratio = if (goal.targetKurus > 0) {
                (goal.savedKurus.toFloat() / goal.targetKurus).coerceIn(0f, 1f)
            } else 0f
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🎯 ${goal.name}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onContribute(goal.id) }) { Text("+ Katkı") }
                        DeleteButton { onDelete(goal.id) }
                    }
                }
                val animatedGoalRatio by animateFloatAsState(targetValue = ratio, label = "goalRatio")
                LinearProgressIndicator(progress = { animatedGoalRatio }, modifier = Modifier.fillMaxWidth())
                Text(
                    "${goal.savedKurus.formatKurusAsTl()} / ${goal.targetKurus.formatKurusAsTl()}  (%${"%.0f".format(ratio * 100)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(row: TxRow, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("${row.emoji} ${row.categoryName}")
            val subtitle = listOfNotNull(
                row.occurredAt.format(txDateFormatter),
                row.note.ifBlank { null },
            ).joinToString(" • ")
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val sign = if (row.type == TxType.EXPENSE) "-" else "+"
            Text(
                "$sign${row.amountKurus.formatKurusAsTl()}",
                color = if (row.type == TxType.EXPENSE) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.tertiary,
            )
            DeleteButton(onDelete)
        }
    }
}
