package com.gym.server.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePlanRequest(
    val name: String,
    val price: Double,
    val durationDays: Int,
    val description: String? = null,
    val weeklyLimit: Int? = null
)
