package com.expensesplitter.app.ui.screens.expense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.Category
import com.expensesplitter.app.data.repository.CurrentUser
import com.expensesplitter.app.data.repository.Expense
import com.expensesplitter.app.data.repository.ExpenseFilter
import com.expensesplitter.app.data.repository.ExpenseRepository
import kotlinx.coroutines.launch

data class ExpenseListUiState(
    val isLoading: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val categoryNames: Map<String, String> = emptyMap(),
    val categories: List<Category> = emptyList(),
    val users: List<CurrentUser> = emptyList(),
    val filter: ExpenseFilter = ExpenseFilter(),
    val error: String? = null,
)

class ExpenseListViewModel(
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    var state by mutableStateOf(ExpenseListUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val categories = expenseRepository.getCategories()
                val users = authRepository.getLoginUsers()
                val expenses = expenseRepository.getExpenses(state.filter)
                state = state.copy(
                    isLoading = false,
                    expenses = expenses,
                    categoryNames = categories.associate { it.id to it.name },
                    categories = categories,
                    users = users,
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Failed to load expenses")
            }
        }
    }

    fun updateFilter(filter: ExpenseFilter) {
        state = state.copy(filter = filter)
        load()
    }
}
