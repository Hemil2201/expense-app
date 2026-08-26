package com.expensesplitter.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_category")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String?,
)
