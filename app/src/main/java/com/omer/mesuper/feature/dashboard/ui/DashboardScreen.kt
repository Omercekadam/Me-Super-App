package com.omer.mesuper.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omer.mesuper.core.ui.formatKurusAsTl
import com.omer.mesuper.feature.activity.ui.ActivityViewModel
import com.omer.mesuper.feature.agenda.ui.AgendaViewModel
import com.omer.mesuper.feature.finance.data.TxType
import com.omer.mesuper.feature.finance.ui.FinanceViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val headerFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("tr", "TR"))
private val txDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("tr", "TR"))

/** Ana sayfa: bugünün özeti. Finans + Ajanda verilerini tek ekranda birleştirir. */
@Composable
fun DashboardScreen(
    vm: FinanceViewModel = hiltViewModel(),
    agendaVm: AgendaViewModel = hiltViewModel(),
    activityVm: ActivityViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val agendaState by agendaVm.uiState.collectAsStateWithLifecycle()
    val activityState by activityVm.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            Text(
                LocalDate.now().format(headerFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Finansal Durum", style = MaterialTheme.typography.titleMedium)
                    val summary = state.summary
                    if (summary == null) {
                        Text(if (state.loading) "Yükleniyor…" else "Henüz işlem yok — + ile başla")
                    } else {
                        Text(
                            summary.balanceKurus.formatKurusAsTl(),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Bu ay gider: ${summary.expenseKurus.formatKurusAsTl()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            state.forecast?.let {
                                Text(
                                    "Ay sonu ~${it.projectedKurus.formatKurusAsTl()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    val generalBudget = state.budgets.firstOrNull { it.row.categoryId == null }
                    if (generalBudget != null) {
                        LinearProgressIndicator(
                            progress = { generalBudget.ratio.coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = when {
                                generalBudget.ratio >= 1f -> MaterialTheme.colorScheme.error
                                generalBudget.ratio >= 0.85f -> Color(0xFFB26A00)
                                else -> MaterialTheme.colorScheme.primary
                            },
                        )
                        Text(
                            "Genel limit: ${generalBudget.spentKurus.formatKurusAsTl()} / ${generalBudget.row.monthlyLimitKurus.formatKurusAsTl()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        val topInsights = state.insights.take(2)
        if (topInsights.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        topInsights.forEach { insight ->
                            val emoji = when (insight.severity) {
                                "alert" -> "🚨"; "warn" -> "⚠️"; else -> "💡"
                            }
                            Text("$emoji ${insight.text}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bugün", style = MaterialTheme.typography.titleMedium)

                    val tickedHabits = agendaState.habits.count { it.tickedToday }
                    val totalHabits = agendaState.habits.size
                    val todayFocusMin = agendaState.pomodoroStats?.todayMinutes ?: 0
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            if (totalHabits > 0) "🔗 Zincir: $tickedHabits/$totalHabits" else "🔗 Alışkanlık yok",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text("⏱ Odaklanma: $todayFocusMin dk", style = MaterialTheme.typography.bodySmall)
                        agendaState.githubStreak?.let {
                            Text("🔥 ${it.currentStreak}", style = MaterialTheme.typography.bodySmall)
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
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Aktivite (son 7 gün)", style = MaterialTheme.typography.titleMedium)
                    val balance = activityState.weeklyBalance
                    if (balance == null || (balance.gameMinutes == 0 && balance.workMinutes == 0)) {
                        Text(
                            "Bu hafta henüz oyun/odaklanma verisi yok.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🎮 ${balance.gameMinutes} dk", style = MaterialTheme.typography.bodySmall)
                            Text("🎯 ${balance.workMinutes} dk", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    activityState.games.filter { it.minutesLast7Days > 0 }.maxByOrNull { it.minutesLast7Days }?.let {
                        Text(
                            "En çok oynanan: ${it.name} (${it.minutesLast7Days} dk)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        val recent = state.transactions.take(3)
        if (recent.isNotEmpty()) {
            item { Text("Son İşlemler", style = MaterialTheme.typography.titleMedium) }
            items(recent, key = { it.id }) { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${row.emoji} ${row.categoryName}")
                    Column(horizontalAlignment = Alignment.End) {
                        val sign = if (row.type == TxType.EXPENSE) "-" else "+"
                        Text(
                            "$sign${row.amountKurus.formatKurusAsTl()}",
                            color = if (row.type == TxType.EXPENSE) Color(0xFFC62828) else Color(0xFF2E7D32),
                        )
                        Text(
                            row.occurredAt.format(txDateFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
