package com.expensesplitter.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val icon: String? = null,
    val is_default: Boolean = false,
)
