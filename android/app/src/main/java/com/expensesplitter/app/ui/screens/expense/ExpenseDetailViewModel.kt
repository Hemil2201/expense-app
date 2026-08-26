package com.expensesplitter.app.ui.screens.expense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.AuthRepository
import com.expensesplitter.app.data.repository.Expense
import com.expensesplitter.app.data.repository.ExpenseComment
import com.expensesplitter.app.data.repository.ExpenseEditHistoryEntry
import com.expensesplitter.app.data.repository.ExpenseRepository
import kotlinx.coroutines.launch

data class ExpenseDetailUiState(
    val isLoading: Boolean = true,
    val expense: Expense? = null,
    val categoryName: String? = null,
    val categoryIcon: String? = null,
    val comments: List<ExpenseComment> = emptyList(),
    val history: List<ExpenseEditHistoryEntry> = emptyList(),
    val userNames: Map<String, String> = emptyMap(),
    val error: String? = null,
)

class ExpenseDetailViewModel(
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository,
    private val expenseId: String,
) : ViewModel() {
    var state by mutableStateOf(ExpenseDetailUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val expense = expenseRepository.getExpense(expenseId)
                val categories = expenseRepository.getCategories()
                val users = authRepository.getLoginUsers()
                val comments = expenseRepository.getComments(expenseId)
                val history = expenseRepository.getEditHistory(expenseId)
                val category = categories.find { it.id == expense.categoryId }
                state = state.copy(
                    isLoading = false,
                    expense = expense,
                    categoryName = category?.name,
                    categoryIcon = category?.icon,
                    comments = comments,
                    history = history,
                    userNames = users.associate { it.id to it.name },
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Failed to load expense")
            }
        }
    }

    fun addComment(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                expenseRepository.addComment(expenseId, text)
                state = state.copy(comments = expenseRepository.getComments(expenseId))
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "Failed to add comment")
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                expenseRepository.deleteExpense(expenseId)
                onDone()
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "Failed to delete")
            }
        }
    }
}
