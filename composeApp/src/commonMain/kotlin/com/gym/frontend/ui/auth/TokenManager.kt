package com.gym.frontend.ui.auth

expect class TokenManager() {
    fun saveToken(token: String)
    fun getToken(): String?
    fun saveRole(role: String)
    fun getRole(): String?
    fun saveUserId(userId: String)
    fun getUserId(): String?
    fun saveUserName(userName: String)
    fun getUserName(): String?
    fun clear()
}
