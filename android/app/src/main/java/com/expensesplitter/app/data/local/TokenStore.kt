package com.expensesplitter.app.data.local

import android.content.Context

/**
 * Session storage for the logged-in user. Plain SharedPreferences is enough
 * here — this is a private 2-user app where the phone itself is the security
 * boundary (see Stage 3 Slice 1 login-method decision).
 */
class TokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun saveSession(token: String, userId: String, name: String, email: String, avatarUrl: String? = null) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_AVATAR_URL, avatarUrl)
            .apply()
    }

    fun updateAvatarUrl(avatarUrl: String?) {
        prefs.edit().putString(KEY_AVATAR_URL, avatarUrl).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun hasSession(): Boolean = getToken() != null

    fun getCachedSessionUser(): SessionUser? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val avatarUrl = prefs.getString(KEY_AVATAR_URL, null)
        return SessionUser(userId, name, email, avatarUrl)
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_AVATAR_URL = "avatar_url"
    }
}

data class SessionUser(val id: String, val name: String, val email: String, val avatarUrl: String? = null)
