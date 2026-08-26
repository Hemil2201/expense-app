package com.expensesplitter.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BudgetSetDto(
    val user_id: String? = null,
    val category_id: String,
    val month: Int,
    val year: Int,
    val target_amount: String,
)

@Serializable
data class BudgetDto(
    val id: String,
    val user_id: String? = null,
    val category_id: String,
    val month: Int,
    val year: Int,
    val target_amount: String,
)

@Serializable
data class PersonalBudgetLineDto(
    val user_id: String,
    val name: String,
    val target_amount: String? = null,
    val actual_spend: String,
)

@Serializable
data class GroupBudgetLineDto(val target_amount: String? = null, val actual_spend: String)

@Serializable
data class CategoryBudgetSummaryDto(
    val category_id: String,
    val category_name: String,
    val personal: List<PersonalBudgetLineDto>,
    val group: GroupBudgetLineDto,
)

@Serializable
data class BudgetSummaryResponseDto(val month: Int, val year: Int, val categories: List<CategoryBudgetSummaryDto>)
