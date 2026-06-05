package com.gym.server.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentRequest(
    val userId: String,
    val amount: Double,
    val method: String
)
