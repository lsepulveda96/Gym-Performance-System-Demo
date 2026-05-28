package com.gym.frontend.ui.demo

import com.gym.shared.domain.UserRole

/**
 * In-memory token store used in DEMO_MODE.
 * Keeps the same interface as the real TokenManager so AuthRepository
 * doesn't need any special casing beyond the two delegate constructors.
 */
class DemoTokenManager {
    private var token:    String? = null
    private var role:     String? = null
    private var userId:   String? = null
    private var userName: String? = null

    fun saveToken(t: String)    { token    = t }
    fun getToken(): String?      = token

    fun saveRole(r: String)     { role     = r }
    fun getRole(): String?       = role

    fun saveUserId(id: String)  { userId   = id }
    fun getUserId(): String?     = userId

    fun saveUserName(n: String) { userName = n }
    fun getUserName(): String?   = userName

    fun clear() { token = null; role = null; userId = null; userName = null }
}
