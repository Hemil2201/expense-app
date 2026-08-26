package com.expensesplitter.app.ui.screens.recurring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.remote.dto.RecurringExpenseCreateDto
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.Category
import com.expensesplitter.app.data.repository.CurrentUser
import com.expensesplitter.app.data.repository.ExpenseRepository
import com.expensesplitter.app.data.repository.RecurringExpense
import com.expensesplitter.app.data.repository.RecurringRepository
import kotlinx.coroutines.launch

data class RecurringUiState(
    val isLoading: Boolean = true,
    val templates: List<RecurringExpense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val users: List<CurrentUser> = emptyList(),
    val error: String? = null,
)

val FREQUENCIES = listOf("weekly", "fortnightly", "monthly", "yearly")

class RecurringViewModel(
    private val recurringRepository: RecurringRepository,
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    var state by mutableStateOf(RecurringUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val templates = recurringRepository.getRecurring()
                val categories = expenseRepository.getCategories()
                val users = authRepository.getLoginUsers()
                state = state.copy(isLoading = false, templates = templates, categories = categories, users = users)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    fun setActive(id: String, active: Boolean) {
        viewModelScope.launch {
            try {
                recurringRepository.setActive(id, active)
                load()
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "Failed to update")
            }
        }
    }

    fun create(body: RecurringExpenseCreateDto, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                recurringRepository.createRecurring(body)
                load()
                onDone()
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "Failed to create")
            }
        }
    }
}
