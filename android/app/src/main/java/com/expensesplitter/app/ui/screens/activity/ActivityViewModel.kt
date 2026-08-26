package com.expensesplitter.app.ui.screens.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.ActivityItem
import com.expensesplitter.app.data.repository.ExpenseRepository
import kotlinx.coroutines.launch

data class ActivityUiState(val isLoading: Boolean = true, val items: List<ActivityItem> = emptyList(), val error: String? = null)

class ActivityViewModel(private val expenseRepository: ExpenseRepository) : ViewModel() {
    var state by mutableStateOf(ActivityUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            state = try {
                state.copy(isLoading = false, items = expenseRepository.getActivity())
            } catch (e: Exception) {
                state.copy(isLoading = false, error = e.message ?: "Failed to load activity")
            }
        }
    }
}
