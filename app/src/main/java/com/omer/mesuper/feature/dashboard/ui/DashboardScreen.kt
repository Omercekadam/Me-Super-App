package com.omer.mesuper.feature.dashboard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omer.mesuper.core.ui.HeroPanel
import com.omer.mesuper.core.ui.LocalModuleColors
import com.omer.mesuper.core.ui.RowDivider
import com.omer.mesuper.core.ui.ScreenTitle
import com.omer.mesuper.core.ui.SectionHeader
import com.omer.mesuper.core.ui.SoftPanel
import com.omer.mesuper.core.ui.formatKurusAsTl
import com.omer.mesuper.feature.activity.ui.ActivityViewModel
import com.omer.mesuper.feature.agenda.ui.AgendaViewModel
import com.omer.mesuper.feature.finance.data.TxType
import com.omer.mesuper.feature.finance.ui.FinanceViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val headerFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("tr", "TR"))
private val txDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("tr", "TR"))

private fun greeting(): String = when (LocalTime.now().hour) {
    in 0..5 -> "İyi geceler"
    in 6..11 -> "Günaydın"
    in 12..17 -> "İyi günler"
    else -> "İyi akşamlar"
}

/** Ana sayfa: bugünün özeti. Finans + Ajanda + Aktivite verilerini tek bütünleşik akışta birleştirir. */
@Composable
fun DashboardScreen(
    vm: FinanceViewModel = hiltViewModel(),
    agendaVm: AgendaViewModel = hiltViewModel(),
    activityVm: ActivityViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val agendaState by agendaVm.uiState.collectAsStateWithLifecycle()
    val activityState by activityVm.uiState.collectAsStateWithLifecycle()
    val modules = LocalModuleColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            ScreenTitle(
                title = greeting(),
                subtitle = LocalDate.now().format(headerFormatter),
            )
        }

        // Odak çıpası: bakiye (solid hero — gradyan yok).
        item {
            HeroPanel(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Text("Finansal Durum", style = MaterialTheme.typography.titleMedium)
                }
                val summary = state.summary
                if (summary == null) {
                    Text(if (state.loading) "Yükleniyor…" else "Henüz işlem yok — + ile başla")
                } else {
                    Text(summary.balanceKurus.formatKurusAsTl(), style = MaterialTheme.typography.displaySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Bu ay gider: ${summary.expenseKurus.formatKurusAsTl()}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        state.forecast?.let {
                            Text(
                                "Ay sonu ~${it.projectedKurus.formatKurusAsTl()}",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }

                val generalBudget = state.budgets.firstOrNull { it.row.categoryId == null }
                if (generalBudget != null) {
                    val onHero = LocalContentColor.current
                    val animatedRatio by animateFloatAsState(
                        targetValue = generalBudget.ratio.coerceAtMost(1f),
                        label = "budgetRatio",
                    )
                    LinearProgressIndicator(
                        progress = { animatedRatio },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (generalBudget.ratio >= 1f) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.secondary,
                        trackColor = onHero.copy(alpha = 0.2f),
                    )
                    Text(
                        "Genel limit: ${generalBudget.spentKurus.formatKurusAsTl()} / ${generalBudget.row.monthlyLimitKurus.formatKurusAsTl()}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        val topInsights = state.insights.take(2)
        if (topInsights.isNotEmpty()) {
            item {
                SoftPanel(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    topInsights.forEach { insight ->
                        val emoji = when (insight.severity) {
                            "alert" -> "🚨"; "warn" -> "⚠️"; else -> "💡"
                        }
                        Text("$emoji ${insight.text}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("Bugün", icon = Icons.Default.Checklist, module = modules.agenda)

                val tickedHabits = agendaState.habits.count { it.tickedToday }
                val totalHabits = agendaState.habits.size
                val todayFocusMin = agendaState.pomodoroStats?.todayMinutes ?: 0
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (totalHabits > 0) "🔗 Zincir: $tickedHabits/$totalHabits" else "🔗 Alışkanlık yok",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text("⏱ Odaklanma: $todayFocusMin dk", style = MaterialTheme.typography.bodyMedium)
                    agendaState.githubStreak?.let {
                        Text("🔥 ${it.currentStreak}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                val openTasks = agendaState.tasks.filter { !it.isDone }.take(3)
                if (openTasks.isEmpty()) {
                    Text(
                        "Açık görev yok 🎉",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    openTasks.forEach { task ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = false, onCheckedChange = { agendaVm.setTaskDone(task, true) })
                            Text(task.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader("Aktivite (son 7 gün)", icon = Icons.Default.SportsEsports, module = modules.activity)
                val balance = activityState.weeklyBalance
                if (balance == null || (balance.gameMinutes == 0 && balance.workMinutes == 0)) {
                    Text(
                        "Bu hafta henüz oyun/odaklanma verisi yok.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🎮 ${balance.gameMinutes} dk", style = MaterialTheme.typography.bodyMedium)
                        Text("🎯 ${balance.workMinutes} dk", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                activityState.games.filter { it.minutesLast7Days > 0 }.maxByOrNull { it.minutesLast7Days }?.let {
                    Text(
                        "En çok oynanan: ${it.name} (${it.minutesLast7Days} dk)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val recent = state.transactions.take(3)
        if (recent.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Son İşlemler", icon = Icons.Default.AccountBalanceWallet, module = modules.finance)
                    recent.forEachIndexed { index, row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${row.emoji} ${row.categoryName}", style = MaterialTheme.typography.bodyMedium)
                            Column(horizontalAlignment = Alignment.End) {
                                val sign = if (row.type == TxType.EXPENSE) "-" else "+"
                                Text(
                                    "$sign${row.amountKurus.formatKurusAsTl()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (row.type == TxType.EXPENSE) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.tertiary,
                                )
                                Text(
                                    row.occurredAt.format(txDateFormatter),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (index < recent.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
