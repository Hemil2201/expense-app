package com.expensesplitter.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptScanResponseDto(
    val date: String? = null,
    val description: String? = null,
    val amount: String? = null,
    val category_id: String? = null,
    val receipt_photo_url: String,
)
