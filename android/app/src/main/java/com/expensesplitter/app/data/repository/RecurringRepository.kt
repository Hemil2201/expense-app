package com.expensesplitter.app.data.repository

import com.expensesplitter.app.data.remote.ApiService
import com.expensesplitter.app.data.remote.dto.RecurringExpenseCreateDto
import com.expensesplitter.app.data.remote.dto.RecurringExpenseUpdateDto

data class RecurringExpense(
    val id: String,
    val amount: String,
    val categoryId: String?,
    val description: String?,
    val paidBy: String,
    val isShared: Boolean,
    val frequency: String,
    val nextRunDate: String,
    val isActive: Boolean,
)

class RecurringRepository(private val apiService: ApiService) {
    suspend fun getRecurring(): List<RecurringExpense> = apiService.getRecurring().map {
        RecurringExpense(it.id, it.amount, it.category_id, it.description, it.paid_by, it.is_shared, it.frequency, it.next_run_date, it.is_active)
    }

    suspend fun createRecurring(body: RecurringExpenseCreateDto) { apiService.createRecurring(body) }

    suspend fun setActive(id: String, active: Boolean) {
        apiService.updateRecurring(id, RecurringExpenseUpdateDto(is_active = active))
    }
}
