package com.gym.server.dto.response

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import com.gym.shared.domain.UserRole

@Serializable
data class MemberResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val joinDate: Instant,
    val status: String,
    val currentPlan: String? = null,
    val phone: String? = null,
    val dni: String? = null,
    val expirationDate: Instant? = null,
    val profileImageUrl: String? = null,
    val weeklyAttendanceCount: Int = 0,
    val weeklyAttendanceLimit: Int? = null
)
