package com.gym.server.dto.response

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class ArrivalResponse(
    val name: String,
    val plan: String,
    val timestamp: Instant
)
