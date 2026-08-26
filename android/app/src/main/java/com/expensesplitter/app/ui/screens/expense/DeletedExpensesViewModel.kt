package com.expensesplitter.app.ui.screens.expense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.Expense
import com.expensesplitter.app.data.repository.ExpenseRepository
import kotlinx.coroutines.launch

data class DeletedExpensesUiState(val isLoading: Boolean = true, val expenses: List<Expense> = emptyList(), val error: String? = null)

class DeletedExpensesViewModel(private val expenseRepository: ExpenseRepository) : ViewModel() {
    var state by mutableStateOf(DeletedExpensesUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            state = try {
                state.copy(isLoading = false, expenses = expenseRepository.getDeletedExpenses())
            } catch (e: Exception) {
                state.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun restore(id: String) {
        viewModelScope.launch {
            try {
                expenseRepository.restoreExpense(id)
                load()
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "Failed to restore")
            }
        }
    }
}
