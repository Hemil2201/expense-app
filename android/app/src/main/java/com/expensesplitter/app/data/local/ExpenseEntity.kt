package com.expensesplitter.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Cache carries listing fields only (offline viewing). Full detail — splits,
// comments, edit history — is fetched live once those screens exist.
@Entity(tableName = "cached_expense")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amount: String,
    val currency: String,
    val date: String,
    val description: String?,
    val categoryId: String?,
    val paidBy: String,
    val isShared: Boolean,
)
