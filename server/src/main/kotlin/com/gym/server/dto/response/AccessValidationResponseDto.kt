package com.gym.server.dto.response

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class AccessValidationResponseDto(
    val success: Boolean,
    val message: String,
    val memberName: String? = null,
    val weeklyAccessLimit: Int? = null,
    val currentWeeklyAccessCount: Int? = null,
    val planName: String? = null,
    val expirationDate: Instant? = null
)
