package com.expensesplitter.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecurringExpenseCreateDto(
    val amount: String,
    val currency: String = "USD",
    val category_id: String? = null,
    val description: String? = null,
    val paid_by: String,
    val is_shared: Boolean = false,
    val split_type: String? = null,
    val splits: List<SplitInputDto>? = null,
    val frequency: String,
    val next_run_date: String,
)

@Serializable
data class RecurringExpenseUpdateDto(
    val amount: String? = null,
    val category_id: String? = null,
    val description: String? = null,
    val is_active: Boolean? = null,
    val next_run_date: String? = null,
)

@Serializable
data class RecurringExpenseDto(
    val id: String,
    val amount: String,
    val currency: String,
    val category_id: String? = null,
    val description: String? = null,
    val paid_by: String,
    val is_shared: Boolean,
    val frequency: String,
    val next_run_date: String,
    val is_active: Boolean,
)
