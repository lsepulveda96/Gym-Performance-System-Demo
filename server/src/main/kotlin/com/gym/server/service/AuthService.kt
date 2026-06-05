package com.gym.server.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.gym.server.repository.UserRepository
import com.gym.shared.domain.AuthResponse
import com.gym.shared.domain.GymUser
import com.gym.shared.domain.LoginRequest
import com.gym.shared.domain.UserRole
import com.gym.shared.domain.result.Result
import java.util.Date

class AuthService(private val userRepository: UserRepository) {
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        val userResult = userRepository.findUserByEmail(request.email)
        if (userResult is Result.Error) {
            return Result.Error(userResult.message, userResult.cause)
        }
        val user = (userResult as Result.Success).data 
            ?: return Result.Error("Invalid credentials")
        
        val isValid = when (user.role) {
            UserRole.MEMBER -> {
                val validatedUserResult = userRepository.validateDni(request.email, request.password)
                if (validatedUserResult is Result.Error) return Result.Error(validatedUserResult.message, validatedUserResult.cause)
                (validatedUserResult as Result.Success).data != null
            }
            UserRole.OWNER, UserRole.RECEPTION -> {
                val validatedUserResult = userRepository.validatePassword(request.email, request.password)
                if (validatedUserResult is Result.Error) return Result.Error(validatedUserResult.message, validatedUserResult.cause)
                (validatedUserResult as Result.Success).data != null
            }
        }
        
        if (isValid) {
            val token = JWT.create()
                .withAudience("gym-system")
                .withIssuer("https://gym-system.com")
                .withClaim("userId", user.id)
                .withClaim("role", user.role.name)
                .withExpiresAt(Date(System.currentTimeMillis() + 2592000000L)) // 30 days
                .sign(Algorithm.HMAC256("secret"))
                
            return Result.Success(AuthResponse(token, user))
        }
        return Result.Error("Invalid credentials")
    }
}
