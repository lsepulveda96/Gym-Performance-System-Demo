package com.gym.server.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyRevenueResponse(
    val month: String,
    val amount: Double
)
