package com.gym.frontend.ui.auth

import kotlinx.browser.localStorage

actual class TokenManager {
    private val TOKEN_KEY = "auth_token"
    private val USER_ROLE_KEY = "user_role"
    private val USER_ID_KEY = "user_id"
    private val USER_NAME_KEY = "user_name"

    private fun safeSet(key: String, value: String) {
        runCatching { localStorage.setItem(key, value) }
    }

    private fun safeGet(key: String): String? {
        return runCatching { localStorage.getItem(key) }.getOrNull()
    }

    private fun safeRemove(key: String) {
        runCatching { localStorage.removeItem(key) }
    }

    actual fun saveToken(token: String) {
        safeSet(TOKEN_KEY, token)
    }

    actual fun getToken(): String? {
        return safeGet(TOKEN_KEY)
    }

    actual fun saveRole(role: String) {
        safeSet(USER_ROLE_KEY, role)
    }

    actual fun getRole(): String? {
        return safeGet(USER_ROLE_KEY)
    }

    actual fun saveUserId(userId: String) {
        safeSet(USER_ID_KEY, userId)
    }

    actual fun getUserId(): String? {
        return safeGet(USER_ID_KEY)
    }

    actual fun saveUserName(userName: String) {
        safeSet(USER_NAME_KEY, userName)
    }

    actual fun getUserName(): String? {
        return safeGet(USER_NAME_KEY)
    }

    actual fun clear() {
        safeRemove(TOKEN_KEY)
        safeRemove(USER_ROLE_KEY)
        safeRemove(USER_ID_KEY)
        safeRemove(USER_NAME_KEY)
    }
}
