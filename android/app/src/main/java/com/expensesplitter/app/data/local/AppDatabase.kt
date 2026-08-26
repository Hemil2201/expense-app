package com.expensesplitter.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, CategoryEntity::class, ExpenseEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense-splitter.db",
                )
                    // Pre-release local dev only: wipe+recreate on schema bump
                    // instead of hand-writing migrations for a schema that's
                    // still moving slice to slice.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
