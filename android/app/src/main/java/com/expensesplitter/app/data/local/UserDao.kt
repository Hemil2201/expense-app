package com.expensesplitter.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM cached_user LIMIT 1")
    suspend fun getCachedUser(): UserEntity?
}
