package com.gym.server.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class AccessValidationRequestDto(
    val code: String
)
