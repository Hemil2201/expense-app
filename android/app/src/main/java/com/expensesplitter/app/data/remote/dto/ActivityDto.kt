package com.expensesplitter.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ActivityItemDto(val type: String, val timestamp: String, val user_name: String, val message: String)

@Serializable
data class ActivityResponseDto(val items: List<ActivityItemDto>)
