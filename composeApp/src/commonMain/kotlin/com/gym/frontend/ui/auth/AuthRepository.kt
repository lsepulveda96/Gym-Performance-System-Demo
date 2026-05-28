package com.gym.frontend.ui.auth

import com.gym.frontend.ui.config.AppConfig
import com.gym.frontend.ui.demo.DemoData
import com.gym.shared.domain.*

class AuthRepository(
    private val authService: AuthService,
    private val tokenManager: TokenManager
) {
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        if (AppConfig.demoMode) {
            val response = when {
                request.email == "admin@demo.com"  && request.password == "1234" -> DemoData.adminAuthResponse
                request.email == "member@demo.com" && request.password == "1234" -> DemoData.memberAuthResponse
                else -> return Result.failure(Exception("Demo: use admin@demo.com or member@demo.com with password 1234"))
            }
            tokenManager.saveToken(response.token)
            tokenManager.saveRole(response.user.role.name)
            tokenManager.saveUserId(response.user.id)
            tokenManager.saveUserName(response.user.name)
            return Result.success(response)
        }
        val result = authService.login(request)
        result.onSuccess { response ->
            tokenManager.saveToken(response.token)
            tokenManager.saveRole(response.user.role.name)
            tokenManager.saveUserId(response.user.id)
            tokenManager.saveUserName(response.user.name)
        }
        return result
    }

    fun getUserName(): String? = tokenManager.getUserName()

    fun isUserLoggedIn(): Boolean = tokenManager.getToken() != null

    fun getUserRole(): UserRole? = tokenManager.getRole()?.let {
        try { UserRole.valueOf(it) } catch (_: Exception) { null }
    }

    fun isSessionActive(): Boolean =
        tokenManager.getToken() != null
            && tokenManager.getRole() != null
            && tokenManager.getUserId() != null

    fun getUserId(): String? = tokenManager.getUserId()

    suspend fun getMe(): Result<Member> {
        if (AppConfig.demoMode) return Result.success(DemoData.memberMe)
        val token = tokenManager.getToken() ?: return Result.failure(Exception("No token found"))
        return authService.getMe(token)
    }

    fun logout() = tokenManager.clear()

    suspend fun updateProfileImage(imageUrl: String): Result<Unit> {
        if (AppConfig.demoMode) return Result.success(Unit)
        val token = tokenManager.getToken() ?: return Result.failure(Exception("No token found"))
        return authService.updateProfileImage(token, imageUrl)
    }
}
