package com.expensesplitter.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryBreakdownDto(val category_id: String? = null, val category_name: String, val total: String)

@Serializable
data class MonthlyReportDto(
    val month: Int,
    val year: Int,
    val total_spend: String,
    val personal_spend: String,
    val shared_spend: String,
    val by_category: List<CategoryBreakdownDto>,
)
