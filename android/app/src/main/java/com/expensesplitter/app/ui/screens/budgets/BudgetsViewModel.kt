package com.expensesplitter.app.ui.screens.budgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.BudgetRepository
import com.expensesplitter.app.data.repository.CategoryBudgetSummary
import java.time.LocalDate
import kotlinx.coroutines.launch

data class BudgetsUiState(
    val month: Int = LocalDate.now().monthValue,
    val year: Int = LocalDate.now().year,
    val isLoading: Boolean = true,
    val categories: List<CategoryBudgetSummary> = emptyList(),
    val error: String? = null,
)

class BudgetsViewModel(private val budgetRepository: BudgetRepository) : ViewModel() {
    var state by mutableStateOf(BudgetsUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            state = try {
                state.copy(isLoading = false, categories = budgetRepository.getSummary(state.month, state.year))
            } catch (e: Exception) {
                state.copy(isLoading = false, error = e.message ?: "Failed to load budgets")
            }
        }
    }

    fun changeMonth(delta: Int) {
        var month = state.month + delta
        var year = state.year
        if (month > 12) { month = 1; year++ }
        if (month < 1) { month = 12; year-- }
        state = state.copy(month = month, year = year)
        load()
    }

    fun setBudget(userId: String?, categoryId: String, amount: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                budgetRepository.setBudget(userId, categoryId, state.month, state.year, amount)
                load()
                onDone()
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "Failed to save target")
            }
        }
    }
}
