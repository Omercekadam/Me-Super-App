package com.omer.mesuper.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.omer.mesuper.feature.finance.data.TxRow
import com.omer.mesuper.feature.finance.data.TxType
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("tr", "TR"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProofScreen(vm: ProofViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Me SuperApp • Faz 0") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryCard(state)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = vm::addRandomExpense, modifier = Modifier.weight(1f)) {
                    Text("Test harcaması")
                }
                OutlinedButton(onClick = vm::addRandomIncome, modifier = Modifier.weight(1f)) {
                    Text("Test geliri")
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.rows, key = { it.id }) { row -> TxRowItem(row) }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: ProofUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val summary = state.summary
            when {
                state.error != null -> {
                    Text("Python köprüsü hatası", style = MaterialTheme.typography.titleMedium)
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                }
                summary == null -> Text("Python motoru ısınıyor…")
                else -> {
                    Text(
                        "Bu Ay (${summary.month})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Gelir: ${summary.incomeKurus.formatKurusAsTl()}")
                        Text("Gider: ${summary.expenseKurus.formatKurusAsTl()}")
                    }
                    Text(
                        "Bakiye: ${summary.balanceKurus.formatKurusAsTl()}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (summary.byCategory.isNotEmpty()) {
                        HorizontalDivider()
                        summary.byCategory.forEach { cat ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(cat.category)
                                Text("${cat.totalKurus.formatKurusAsTl()}  (%${"%.1f".format(cat.share * 100)})")
                            }
                        }
                    }
                    Text(
                        "⚙ ${summary.engine} hesapladı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun TxRowItem(row: TxRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${row.emoji} ${row.categoryName}", style = MaterialTheme.typography.bodyLarge)
        Column(horizontalAlignment = Alignment.End) {
            val sign = if (row.type == TxType.EXPENSE) "-" else "+"
            Text(
                "$sign${row.amountKurus.formatKurusAsTl()}",
                color = if (row.type == TxType.EXPENSE) Color(0xFFC62828) else Color(0xFF2E7D32),
            )
            Text(
                row.occurredAt.format(dateFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
