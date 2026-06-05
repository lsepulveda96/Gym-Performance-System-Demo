package com.gym.server.dto.response

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class PaymentResponse(
    val id: String,
    val userId: String,
    val amount: Double,
    val paymentDate: Instant,
    val expirationDate: Instant,
    val method: String
)
