package com.expensesplitter.app.ui.screens.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expensesplitter.app.data.repository.Expense
import com.expensesplitter.app.data.repository.ExpenseRepository
import com.expensesplitter.app.ui.theme.Spacing

@Composable
fun DeletedExpensesScreen(expenseRepository: ExpenseRepository) {
    val viewModel: DeletedExpensesViewModel = viewModel(
        factory = viewModelFactory { initializer { DeletedExpensesViewModel(expenseRepository) } },
    )
    val state = viewModel.state

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.padding(24.dp)) }
            state.expenses.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing here", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = Spacing.lg)) {
                items(state.expenses, key = { it.id }) { expense ->
                    DeletedExpenseRow(expense) { viewModel.restore(expense.id) }
                    Divider(modifier = Modifier.padding(horizontal = Spacing.lg))
                }
            }
        }
    }
}

@Composable
private fun DeletedExpenseRow(expense: Expense, onRestore: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(expense.description ?: "(no description)", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${expense.date} · ${expense.currency} ${expense.amount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onRestore) { Text("Restore") }
    }
}
