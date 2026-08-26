package com.expensesplitter.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserBalanceDto(val user_id: String, val name: String, val net_balance: String)

@Serializable
data class BalanceResponseDto(val balances: List<UserBalanceDto>)
