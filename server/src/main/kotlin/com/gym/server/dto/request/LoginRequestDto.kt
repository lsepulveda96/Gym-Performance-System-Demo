package com.gym.server.dto.request

import kotlinx.serialization.Serializable
import com.gym.shared.domain.UserRole

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    val role: UserRole
)
