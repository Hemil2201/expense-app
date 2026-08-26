package com.expensesplitter.app.ui.screens.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.ExpenseRepository
import com.expensesplitter.app.data.repository.PendingReceiptDraftHolder
import com.expensesplitter.app.ui.components.CategoryIcon
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    expenseRepository: ExpenseRepository,
    authRepository: AuthRepository,
    pendingReceiptDraftHolder: PendingReceiptDraftHolder? = null,
    onSaved: () -> Unit,
) {
    val viewModel: AddExpenseViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AddExpenseViewModel(expenseRepository, authRepository, pendingReceiptDraftHolder) }
        },
    )
    val state = viewModel.state

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.padding(24.dp)) }
        return
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.receiptPhotoUrl != null) {
            Text("📎 Receipt attached — review the fields below before saving", style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
            value = state.amount,
            onValueChange = viewModel::updateAmount,
            placeholder = { Text("0.00", style = MaterialTheme.typography.displaySmall) },
            textStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold),
            leadingIcon = { Text(state.currency, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CURRENCIES.forEach { currency ->
                FilterChip(
                    selected = state.currency == currency,
                    onClick = { viewModel.selectCurrency(currency) },
                    label = { Text(currency) },
                )
            }
        }

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::updateDescription,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(onClick = { showDatePicker = true }) { Text("Date: ${state.date}") }

        CategoryDropdown(
            categories = state.categories,
            selectedId = state.selectedCategoryId,
            onSelect = viewModel::selectCategory,
        )

        Text("Paid by", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.users.forEach { user ->
                FilterChip(
                    selected = state.paidBy == user.id,
                    onClick = { viewModel.selectPaidBy(user.id) },
                    label = { Text(user.name) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Shared expense", style = MaterialTheme.typography.labelLarge)
            Switch(checked = state.isShared, onCheckedChange = viewModel::toggleShared)
        }

        if (state.isShared) {
            Text("Split type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SPLIT_TYPES.forEach { type ->
                    FilterChip(
                        selected = state.splitType == type,
                        onClick = { viewModel.selectSplitType(type) },
                        label = { Text(type.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            if (state.splitType != "equal") {
                val hint = when (state.splitType) {
                    "exact" -> "Dollar amount each person owes (must sum to the total)"
                    "percentage" -> "Percentage each person owes (must sum to 100)"
                    else -> "Share count for each person (e.g. 2 and 1)"
                }
                Text(hint, style = MaterialTheme.typography.bodySmall)
                state.users.forEach { user ->
                    OutlinedTextField(
                        value = state.splitValues[user.id] ?: "",
                        onValueChange = { viewModel.updateSplitValue(user.id, it) },
                        label = { Text(user.name) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { viewModel.submit(onSaved) },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isSubmitting) "Saving…" else "Save Expense") }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<com.expensesplitter.app.data.repository.Category>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.find { it.id == selectedId }
    val selectedName = selected?.name ?: "Select category"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            leadingIcon = { CategoryIcon(categoryName = selected?.name ?: "?", icon = selected?.icon, size = 28.dp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    leadingIcon = { CategoryIcon(categoryName = category.name, icon = category.icon, size = 28.dp) },
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
