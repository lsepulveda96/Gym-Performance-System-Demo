package com.gym.server.dto.response

import kotlinx.serialization.Serializable
import com.gym.shared.domain.UserRole

@Serializable
data class GymUserResponse(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val profileImageUrl: String? = null
)
