package com.expensesplitter.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("SELECT * FROM cached_category ORDER BY name")
    suspend fun getAll(): List<CategoryEntity>

    @Query("DELETE FROM cached_category")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>) {
        clear()
        upsertAll(categories)
    }
}
