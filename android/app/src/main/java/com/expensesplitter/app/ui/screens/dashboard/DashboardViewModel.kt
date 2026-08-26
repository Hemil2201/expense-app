package com.expensesplitter.app.ui.screens.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.ActivityItem
import com.expensesplitter.app.data.repository.Category
import com.expensesplitter.app.data.repository.ExpenseRepository
import com.expensesplitter.app.data.repository.UserBalance
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val balances: List<UserBalance> = emptyList(),
    val activity: List<ActivityItem> = emptyList(),
    val categories: List<Category> = emptyList(),
)

class DashboardViewModel(private val expenseRepository: ExpenseRepository) : ViewModel() {
    var state by mutableStateOf(DashboardUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val balances = try {
                expenseRepository.getBalance()
            } catch (e: Exception) {
                emptyList()
            }
            val activity = try {
                expenseRepository.getActivity()
            } catch (e: Exception) {
                emptyList()
            }
            val categories = try {
                expenseRepository.getCategories()
            } catch (e: Exception) {
                emptyList()
            }
            state = DashboardUiState(isLoading = false, balances = balances, activity = activity, categories = categories)
        }
    }
}
