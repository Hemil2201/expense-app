package com.expensesplitter.app.data.repository

import com.expensesplitter.app.data.remote.ApiService
import com.expensesplitter.app.data.remote.dto.BudgetSetDto

data class PersonalBudgetLine(val userId: String, val name: String, val targetAmount: String?, val actualSpend: String)

data class GroupBudgetLine(val targetAmount: String?, val actualSpend: String)

data class CategoryBudgetSummary(
    val categoryId: String,
    val categoryName: String,
    val personal: List<PersonalBudgetLine>,
    val group: GroupBudgetLine,
)

class BudgetRepository(private val apiService: ApiService) {
    suspend fun getSummary(month: Int, year: Int): List<CategoryBudgetSummary> =
        apiService.getBudgetSummary(month, year).categories.map {
            CategoryBudgetSummary(
                categoryId = it.category_id,
                categoryName = it.category_name,
                personal = it.personal.map { p -> PersonalBudgetLine(p.user_id, p.name, p.target_amount, p.actual_spend) },
                group = GroupBudgetLine(it.group.target_amount, it.group.actual_spend),
            )
        }

    suspend fun setBudget(userId: String?, categoryId: String, month: Int, year: Int, targetAmount: String) {
        apiService.setBudget(BudgetSetDto(userId, categoryId, month, year, targetAmount))
    }
}
