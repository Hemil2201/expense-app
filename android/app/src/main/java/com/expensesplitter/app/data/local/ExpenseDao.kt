package com.expensesplitter.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface ExpenseDao {
    @Upsert
    suspend fun upsertAll(expenses: List<ExpenseEntity>)

    @Query("SELECT * FROM cached_expense ORDER BY date DESC")
    suspend fun getAll(): List<ExpenseEntity>

    @Query("DELETE FROM cached_expense")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(expenses: List<ExpenseEntity>) {
        clear()
        upsertAll(expenses)
    }
}
