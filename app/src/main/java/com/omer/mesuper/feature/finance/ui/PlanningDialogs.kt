package com.omer.mesuper.feature.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omer.mesuper.core.ui.parseTlToKurus
import com.omer.mesuper.feature.finance.data.CategoryEntity
import com.omer.mesuper.feature.finance.data.SubscriptionPeriod
import com.omer.mesuper.feature.finance.data.TxType

/** Bütçe limiti ekleme: kategori (veya Genel) + aylık limit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddBudgetDialog(
    categories: List<CategoryEntity>,
    onConfirm: (categoryId: Long?, limitKurus: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var limitText by rememberSaveable { mutableStateOf("") }
    val limitKurus = parseTlToKurus(limitText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Harcama Limiti") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("🧮 Genel") },
                    )
                    categories.filter { it.type == TxType.EXPENSE }.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text("${cat.emoji} ${cat.name}") },
                        )
                    }
                }
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Aylık limit (TL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = limitKurus != null && limitKurus > 0,
                onClick = { onConfirm(selectedCategoryId, limitKurus!!); onDismiss() },
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

/** Abonelik ekleme: ad, tutar, yenilenme günü, periyot. */
@Composable
fun AddSubscriptionDialog(
    onConfirm: (name: String, amountKurus: Long, billingDay: Int, period: SubscriptionPeriod, billingMonth: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var dayText by rememberSaveable { mutableStateOf("1") }
    var monthText by rememberSaveable { mutableStateOf("1") }
    var period by rememberSaveable { mutableStateOf(SubscriptionPeriod.MONTHLY) }

    val amountKurus = parseTlToKurus(amountText)
    val day = dayText.toIntOrNull()
    val month = monthText.toIntOrNull()
    val valid = name.isNotBlank() && amountKurus != null && amountKurus > 0 &&
        day != null && day in 1..28 &&
        (period == SubscriptionPeriod.MONTHLY || (month != null && month in 1..12))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonelik") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Ad (ör. Spotify)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it },
                    label = { Text("Tutar (TL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = period == SubscriptionPeriod.MONTHLY,
                        onClick = { period = SubscriptionPeriod.MONTHLY },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Aylık") }
                    SegmentedButton(
                        selected = period == SubscriptionPeriod.YEARLY,
                        onClick = { period = SubscriptionPeriod.YEARLY },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("Yıllık") }
                }
                OutlinedTextField(
                    value = dayText, onValueChange = { dayText = it },
                    label = { Text("Yenilenme günü (1-28)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                if (period == SubscriptionPeriod.YEARLY) {
                    OutlinedTextField(
                        value = monthText, onValueChange = { monthText = it },
                        label = { Text("Yenilenme ayı (1-12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(
                        name.trim(), amountKurus!!, day!!, period,
                        if (period == SubscriptionPeriod.YEARLY) month else null,
                    )
                    onDismiss()
                },
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

/** Birikim hedefi ekleme. */
@Composable
fun AddGoalDialog(
    onConfirm: (name: String, targetKurus: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var targetText by rememberSaveable { mutableStateOf("") }
    val targetKurus = parseTlToKurus(targetText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Birikim Hedefi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Ad (ör. Yeni ekran kartı)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetText, onValueChange = { targetText = it },
                    label = { Text("Hedef tutar (TL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && targetKurus != null && targetKurus > 0,
                onClick = { onConfirm(name.trim(), targetKurus!!); onDismiss() },
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

/** Hedefe katkı ekleme. */
@Composable
fun ContributeDialog(
    goalName: String,
    onConfirm: (amountKurus: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    val amountKurus = parseTlToKurus(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Katkı: $goalName") },
        text = {
            OutlinedTextField(
                value = amountText, onValueChange = { amountText = it },
                label = { Text("Tutar (TL)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = amountKurus != null && amountKurus > 0,
                onClick = { onConfirm(amountKurus!!); onDismiss() },
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
