package com.expensesplitter.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val user_id: String, val pin: String)

@Serializable
data class TokenResponseDto(
    val access_token: String,
    val token_type: String,
    val user: UserDto,
)
