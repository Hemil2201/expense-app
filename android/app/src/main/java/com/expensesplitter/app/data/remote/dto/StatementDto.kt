package com.expensesplitter.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatementUploadDto(
    val id: String,
    val uploaded_by: String,
    val file_url: String,
    val bank_name: String? = null,
    val card_last4: String? = null,
    val upload_date: String,
    val status: String,
)

@Serializable
data class StatementUploadSummaryDto(
    val id: String,
    val status: String,
    val total: Int,
    val resolved: Int,
    val needs_clarification: Int,
    val possible_duplicates: Int,
    val expected_total: Int? = null,
)

@Serializable
data class StatementTransactionDto(
    val id: String,
    val statement_upload_id: String,
    val raw_date: String? = null,
    val raw_description: String? = null,
    val raw_amount: String? = null,
    val matched_category_id: String? = null,
    val needs_clarification: Boolean,
    val user_clarification_note: String? = null,
    val is_duplicate_of: String? = null,
    val resolved_expense_id: String? = null,
)

@Serializable
data class ResolveTransactionDto(
    val category_id: String? = null,
    val paid_by: String? = null,
    val is_shared: Boolean = false,
    val split_type: String? = null,
    val splits: List<SplitInputDto>? = null,
    val user_clarification_note: String? = null,
    val confirm_duplicate: Boolean = false,
)
