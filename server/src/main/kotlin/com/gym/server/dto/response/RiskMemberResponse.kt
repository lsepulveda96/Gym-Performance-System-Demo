package com.gym.server.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class RiskMemberResponse(
    val name: String,
    val daysRemaining: Int,
    val amount: Double
)
