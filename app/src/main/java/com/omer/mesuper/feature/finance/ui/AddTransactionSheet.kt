package com.omer.mesuper.feature.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.omer.mesuper.feature.finance.data.TxType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val sheetDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr", "TR"))

/**
 * Hızlı işlem girişi formu. ModalBottomSheet içinde gösterilir;
 * kaydetme başarılıysa [onSaved] çağrılır (sheet kapatılır).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionForm(
    categories: List<CategoryEntity>,
    onSave: (amountKurus: Long, type: TxType, categoryId: Long, date: LocalDate, note: String) -> Unit,
    onSaved: () -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val visibleCategories = categories.filter { it.type == type }
    val amountKurus = parseTlToKurus(amountText)
    val canSave = amountKurus != null && amountKurus > 0 && selectedCategoryId != null

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Yeni İşlem", style = MaterialTheme.typography.titleLarge)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = type == TxType.EXPENSE,
                onClick = { type = TxType.EXPENSE; selectedCategoryId = null },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Gider") }
            SegmentedButton(
                selected = type == TxType.INCOME,
                onClick = { type = TxType.INCOME; selectedCategoryId = null },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Gelir") }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Tutar (TL)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = amountText.isNotBlank() && amountKurus == null,
            modifier = Modifier.fillMaxWidth(),
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            visibleCategories.forEach { cat ->
                FilterChip(
                    selected = selectedCategoryId == cat.id,
                    onClick = { selectedCategoryId = cat.id },
                    label = { Text("${cat.emoji} ${cat.name}") },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = { showDatePicker = true }) {
                Text("📅 ${LocalDate.parse(date).format(sheetDateFormatter)}")
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Not (isteğe bağlı)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                onSave(amountKurus!!, type, selectedCategoryId!!, LocalDate.parse(date), note.trim())
                onSaved()
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Kaydet") }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(date)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Vazgeç") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
