package com.gym.server.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class PlanResponse(
    val id: String,
    val name: String,
    val price: Double,
    val durationDays: Int,
    val description: String? = null,
    val weeklyLimit: Int? = null
)
