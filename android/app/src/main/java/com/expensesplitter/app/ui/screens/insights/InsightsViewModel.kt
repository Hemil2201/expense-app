package com.expensesplitter.app.ui.screens.insights

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensesplitter.app.data.repository.BudgetRepository
import com.expensesplitter.app.data.repository.CategoryBudgetSummary
import com.expensesplitter.app.data.repository.CategoryBreakdown
import com.expensesplitter.app.data.repository.MonthlyReport
import com.expensesplitter.app.data.repository.ReportRepository
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class MonthSpend(val month: Int, val year: Int, val total: String)

data class InsightsUiState(
    val month: Int = LocalDate.now().monthValue,
    val year: Int = LocalDate.now().year,
    val isLoading: Boolean = true,
    val report: MonthlyReport? = null,
    val budgets: List<CategoryBudgetSummary> = emptyList(),
    val trend: List<MonthSpend> = emptyList(),
    val error: String? = null,
) {
    val byCategory: List<CategoryBreakdown> get() = report?.byCategory.orEmpty()
}

private const val TREND_MONTHS = 6

class InsightsViewModel(
    private val reportRepository: ReportRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {
    var state by mutableStateOf(InsightsUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val report = reportRepository.getMonthlyReport(state.month, state.year)
                val budgets = budgetRepository.getSummary(state.month, state.year)
                val trend = loadTrend(state.month, state.year)
                state = state.copy(isLoading = false, report = report, budgets = budgets, trend = trend)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Failed to load insights")
            }
        }
    }

    private suspend fun loadTrend(month: Int, year: Int): List<MonthSpend> = coroutineScope {
        val months = (0 until TREND_MONTHS).map { offset ->
            var m = month - (TREND_MONTHS - 1 - offset)
            var y = year
            while (m < 1) { m += 12; y-- }
            m to y
        }
        months.map { (m, y) ->
            async { MonthSpend(m, y, reportRepository.getMonthlyReport(m, y).totalSpend) }
        }.awaitAll()
    }

    fun changeMonth(delta: Int) {
        var month = state.month + delta
        var year = state.year
        if (month > 12) { month = 1; year++ }
        if (month < 1) { month = 12; year-- }
        state = state.copy(month = month, year = year)
        load()
    }
}
