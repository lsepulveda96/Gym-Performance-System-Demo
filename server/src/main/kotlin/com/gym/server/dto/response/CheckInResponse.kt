package com.gym.server.dto.response

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class CheckInResponse(
    val id: String,
    val userId: String,
    val timestamp: Instant,
    val planIdAtTime: String
)
