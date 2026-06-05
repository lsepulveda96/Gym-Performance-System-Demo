package com.gym.server.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    val token: String,
    val user: GymUserResponse
)
