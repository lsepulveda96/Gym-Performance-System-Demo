package com.gym.server.dto.response

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class QRTokenResponse(
    val token: String,
    val expiresAt: Instant
)
