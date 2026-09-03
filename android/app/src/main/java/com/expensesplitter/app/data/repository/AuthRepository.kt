package com.expensesplitter.app.data.repository

import com.expensesplitter.app.data.local.TokenStore
import com.expensesplitter.app.data.local.UserDao
import com.expensesplitter.app.data.local.UserEntity
import com.expensesplitter.app.data.remote.ApiService
import com.expensesplitter.app.data.remote.dto.LoginRequestDto
import com.expensesplitter.app.data.remote.dto.UserDto
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

data class CurrentUser(val id: String, val name: String, val email: String, val avatarUrl: String?)

class AuthRepository(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val tokenStore: TokenStore,
) {
    suspend fun getLoginUsers(): List<CurrentUser> = apiService.getLoginUsers().map { it.toCurrentUser() }

    suspend fun login(userId: String, pin: String): CurrentUser {
        val response = apiService.login(LoginRequestDto(user_id = userId, pin = pin))
        tokenStore.saveSession(
            token = response.access_token,
            userId = response.user.id,
            name = response.user.name,
            email = response.user.email,
            avatarUrl = response.user.avatar_url,
        )
        userDao.upsert(response.user.toEntity())
        return response.user.toCurrentUser()
    }

    fun logout() = tokenStore.clearSession()

    fun hasSession(): Boolean = tokenStore.hasSession()

    fun getSessionUser(): CurrentUser? = tokenStore.getCachedSessionUser()?.let {
        CurrentUser(id = it.id, name = it.name, email = it.email, avatarUrl = it.avatarUrl)
    }

    suspend fun uploadAvatar(file: File, mimeType: String): CurrentUser {
        val part = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
        val dto = apiService.updateAvatar(part)
        tokenStore.updateAvatarUrl(dto.avatar_url)
        userDao.upsert(dto.toEntity())
        return dto.toCurrentUser()
    }

    private fun UserDto.toCurrentUser() = CurrentUser(id = id, name = name, email = email, avatarUrl = avatar_url)
    private fun UserDto.toEntity() = UserEntity(id = id, name = name, email = email, avatarUrl = avatar_url)
}
