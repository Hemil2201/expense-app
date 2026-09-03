package com.expensesplitter.app.ui.screens.expense

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.Expense
import com.expensesplitter.app.data.repository.ExpenseFilter
import com.expensesplitter.app.data.repository.ExpenseRepository
import com.expensesplitter.app.data.repository.PendingCategoryFilterHolder
import com.expensesplitter.app.ui.components.CategoryIcon
import com.expensesplitter.app.ui.components.EmptyState
import com.expensesplitter.app.ui.components.SkeletonList
import com.expensesplitter.app.ui.theme.BalanceColors
import com.expensesplitter.app.ui.theme.MoneyType
import com.expensesplitter.app.ui.theme.Spacing
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    expenseRepository: ExpenseRepository,
    authRepository: AuthRepository,
    pendingCategoryFilterHolder: PendingCategoryFilterHolder,
    onAddExpense: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onViewDeleted: () -> Unit,
) {
    val viewModel: ExpenseListViewModel = viewModel(
        factory = viewModelFactory { initializer { ExpenseListViewModel(expenseRepository, authRepository) } },
    )
    val state = viewModel.state
    val sessionUserId = authRepository.getSessionUser()?.id
    var datePickerTarget by remember { mutableStateOf<String?>(null) } // "start" | "end" | null

    // Re-fetch whenever this screen comes back into view (e.g. after deleting
    // or editing an expense on the detail screen) — the ViewModel survives
    // across that round trip, so init{} alone won't pick up the change. A
    // category tapped on Home's grid also arrives here via the pending
    // holder rather than a nav argument.
    LaunchedEffect(Unit) {
        val pendingCategoryId = pendingCategoryFilterHolder.consume()
        if (pendingCategoryId != null) {
            viewModel.updateFilter(state.filter.copy(categoryId = pendingCategoryId))
        } else {
            viewModel.load()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FilterRow(
                filter = state.filter,
                categories = state.categories,
                users = state.users,
                onFilterChange = viewModel::updateFilter,
                onPickStartDate = { datePickerTarget = "start" },
                onPickEndDate = { datePickerTarget = "end" },
            )
            TextButton(onClick = onViewDeleted, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("Recently Deleted", style = MaterialTheme.typography.labelMedium)
            }

            Crossfade(targetState = state.isLoading, animationSpec = tween(250), label = "expenses-loading") { loading ->
                when {
                    loading -> SkeletonList(rows = 6)
                    state.error != null -> Box(Modifier.fillMaxSize()) {
                        Text("Couldn't load expenses: ${state.error}", modifier = Modifier.padding(24.dp))
                    }
                    state.expenses.isEmpty() -> EmptyState(
                        icon = Icons.Filled.ReceiptLong,
                        title = "No expenses yet",
                        message = "Add your first expense with the + button, or try adjusting your filters.",
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(state.expenses) { expense ->
                            ExpenseRow(
                                expense = expense,
                                category = state.categories.find { it.id == expense.categoryId },
                                sessionUserId = sessionUserId,
                                onClick = { onOpenExpense(expense.id) },
                            )
                            Divider(modifier = Modifier.padding(horizontal = Spacing.lg))
                        }
                    }
                }
            }
        }
    }

    datePickerTarget?.let { target ->
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    val date = millis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    val newFilter = if (target == "start") {
                        state.filter.copy(startDate = date?.toString())
                    } else {
                        state.filter.copy(endDate = date?.toString())
                    }
                    viewModel.updateFilter(newFilter)
                    datePickerTarget = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { datePickerTarget = null }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun FilterRow(
    filter: ExpenseFilter,
    categories: List<com.expensesplitter.app.data.repository.Category>,
    users: List<com.expensesplitter.app.data.repository.CurrentUser>,
    onFilterChange: (ExpenseFilter) -> Unit,
    onPickStartDate: () -> Unit,
    onPickEndDate: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FilterChip(selected = filter.startDate != null, onClick = onPickStartDate, label = { Text(filter.startDate ?: "From") })
            FilterChip(selected = filter.endDate != null, onClick = onPickEndDate, label = { Text(filter.endDate ?: "To") })

            FilterChip(
                selected = filter.isShared == false,
                onClick = { onFilterChange(filter.copy(isShared = if (filter.isShared == false) null else false)) },
                label = { Text("Personal") },
            )
            FilterChip(
                selected = filter.isShared == true,
                onClick = { onFilterChange(filter.copy(isShared = if (filter.isShared == true) null else true)) },
                label = { Text("Shared") },
            )
            users.forEach { user ->
                FilterChip(
                    selected = filter.personId == user.id,
                    onClick = { onFilterChange(filter.copy(personId = if (filter.personId == user.id) null else user.id)) },
                    label = { Text(user.name) },
                )
            }
            categories.forEach { category ->
                FilterChip(
                    selected = filter.categoryId == category.id,
                    onClick = {
                        onFilterChange(filter.copy(categoryId = if (filter.categoryId == category.id) null else category.id))
                    },
                    label = { Text(category.name) },
                )
            }
            if (!filter.isEmpty) {
                TextButton(onClick = { onFilterChange(ExpenseFilter()) }) { Text("Clear") }
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    category: com.expensesplitter.app.data.repository.Category?,
    sessionUserId: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            CategoryIcon(categoryName = category?.name ?: "Other", icon = category?.icon)
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    expense.description ?: "(no description)",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${expense.date} · ${category?.name ?: "Uncategorized"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val mySplit = expense.splits.find { it.userId == sessionUserId }?.amountOwed?.toBigDecimalOrNull()
        val fullAmount = expense.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = Spacing.sm)) {
            if (expense.isShared && mySplit != null) {
                val iPaid = expense.paidBy == sessionUserId
                val netToMe = if (iPaid) fullAmount - mySplit else -mySplit
                val label = if (netToMe >= BigDecimal.ZERO) "you get back" else "you owe"
                val color = if (netToMe >= BigDecimal.ZERO) BalanceColors.positiveLight else BalanceColors.negativeLight
                Text(
                    "${expense.currency} ${netToMe.abs()}",
                    style = MoneyType.small,
                    color = color,
                )
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("${expense.currency} ${expense.amount}", style = MoneyType.small, color = MaterialTheme.colorScheme.onBackground)
                Text("personal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? = try {
    BigDecimal(this)
} catch (e: NumberFormatException) {
    null
}
