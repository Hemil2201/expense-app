package com.expensesplitter.app.ui.screens.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expensesplitter.app.data.remote.dto.RecurringExpenseCreateDto
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.Category
import com.expensesplitter.app.data.repository.ExpenseRepository
import com.expensesplitter.app.data.repository.RecurringExpense
import com.expensesplitter.app.data.repository.RecurringRepository
import com.expensesplitter.app.ui.components.CategoryIcon
import com.expensesplitter.app.ui.components.EmptyState
import com.expensesplitter.app.ui.components.SkeletonList
import com.expensesplitter.app.ui.theme.Spacing
import com.expensesplitter.app.ui.util.sanitizeMoneyInput
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    recurringRepository: RecurringRepository,
    expenseRepository: ExpenseRepository,
    authRepository: AuthRepository,
) {
    val viewModel: RecurringViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RecurringViewModel(recurringRepository, expenseRepository, authRepository) }
        },
    )
    val state = viewModel.state
    var showCreate by remember { mutableStateOf(false) }

    if (showCreate) {
        CreateRecurringForm(
            categories = state.categories,
            users = state.users,
            onCancel = { showCreate = false },
            onCreate = { body -> viewModel.create(body) { showCreate = false } },
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add recurring expense")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = state.isLoading, animationSpec = tween(250), label = "recurring-loading") { loading ->
                when {
                    loading -> SkeletonList(rows = 4)
                    state.templates.isEmpty() -> EmptyState(
                        icon = Icons.Filled.Autorenew,
                        title = "No recurring expenses yet",
                        message = "Set up rent, subscriptions, or other bills that repeat every month.",
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(state.templates, key = { it.id }) { template ->
                            RecurringRow(
                                template = template,
                                category = state.categories.find { it.id == template.categoryId },
                                onToggle = { active -> viewModel.setActive(template.id, active) },
                            )
                            Divider(modifier = Modifier.padding(horizontal = Spacing.lg))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringRow(template: RecurringExpense, category: Category?, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(categoryName = category?.name ?: "Other", icon = category?.icon)
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(template.description ?: "(no description)", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${template.amount} · ${template.frequency} · next: ${template.nextRunDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = template.isActive, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRecurringForm(
    categories: List<com.expensesplitter.app.data.repository.Category>,
    users: List<com.expensesplitter.app.data.repository.CurrentUser>,
    onCancel: () -> Unit,
    onCreate: (RecurringExpenseCreateDto) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }
    var paidBy by remember { mutableStateOf(users.firstOrNull()?.id) }
    var isShared by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf("monthly") }
    var nextRunDate by remember { mutableStateOf(LocalDate.now()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg).imePadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("New Recurring Expense", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = sanitizeMoneyInput(it) },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

        val selectedCategory = categories.find { it.id == selectedCategoryId }
        ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "Select category",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                leadingIcon = {
                    CategoryIcon(categoryName = selectedCategory?.name ?: "?", icon = selectedCategory?.icon, size = 28.dp)
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        leadingIcon = { CategoryIcon(categoryName = category.name, icon = category.icon, size = 28.dp) },
                        text = { Text(category.name) },
                        onClick = { selectedCategoryId = category.id; categoryMenuExpanded = false },
                    )
                }
            }
        }

        Text("Paid by", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            users.forEach { user ->
                FilterChip(selected = paidBy == user.id, onClick = { paidBy = user.id }, label = { Text(user.name) })
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Shared expense (equal split)")
            Switch(checked = isShared, onCheckedChange = { isShared = it })
        }

        Text("Frequency", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FREQUENCIES.forEach { freq ->
                FilterChip(selected = frequency == freq, onClick = { frequency = freq }, label = { Text(freq.replaceFirstChar { it.uppercase() }) })
            }
        }

        TextButton(onClick = { showDatePicker = true }) { Text("Next run date: $nextRunDate") }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(onClick = {
                val paidById = paidBy ?: return@Button
                onCreate(
                    RecurringExpenseCreateDto(
                        amount = amount,
                        category_id = selectedCategoryId,
                        description = description.ifBlank { null },
                        paid_by = paidById,
                        is_shared = isShared,
                        split_type = if (isShared) "equal" else null,
                        frequency = frequency,
                        next_run_date = nextRunDate.toString(),
                    ),
                )
            }) { Text("Create") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = nextRunDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        nextRunDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }
}
