package com.gym.server.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateMemberRequest(
    val name: String,
    val email: String,
    val dni: String,
    val phone: String?,
    val planId: String?,
    val isActive: Boolean = true
)
