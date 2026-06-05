package com.gym.server.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateMemberRequest(
    val name: String,
    val email: String,
    val dni: String,
    val phone: String?,
    val planId: String?,
    val isActive: Boolean = true,
    val paymentAmount: Double,
    val paymentMethod: String
)
