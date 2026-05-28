package com.gym.shared.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
enum class UserRole {
    OWNER, RECEPTION, MEMBER
}

@Serializable
data class GymUser(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val profileImageUrl: String? = null
)

@Serializable
data class MemberProfile(
    val userId: String,
    val phone: String?,
    val joinDate: Instant,
    val isActive: Boolean = true,
    val currentPlanId: String? = null
)

@Serializable
data class GymPlan(
    val id: String,
    val name: String,
    val price: Double,
    val durationDays: Int,
    val description: String? = null,
    val weeklyLimit: Int? = null
)

@Serializable
data class SubscriptionDetails(
    val id: String,
    val userId: String,
    val planId: String,
    val startDate: Instant,
    val endDate: Instant,
    val status: SubscriptionStatus
)

@Serializable
enum class SubscriptionStatus {
    ACTIVE, EXPIRED, CANCELLED, OVERDUE
}

@Serializable
data class CheckIn(
    val id: String,
    val userId: String,
    val timestamp: Instant,
    val planIdAtTime: String
)

@Serializable
data class QRToken(
    val token: String,
    val expiresAt: Instant
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val role: UserRole
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: GymUser
)

@Serializable
data class Member(
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

@Serializable
data class MemberRequest(
    val name: String,
    val email: String,
    val dni: String,
    val phone: String?,
    val planId: String?,
    val isActive: Boolean = true,
    val paymentAmount: Double,
    val paymentMethod: String
)
@Serializable
data class AccessValidationRequest(
    val code: String
)

@Serializable
data class AccessValidationResponse(
    val success: Boolean,
    val message: String,
    val memberName: String? = null,
    val weeklyAccessLimit: Int? = null,
    val currentWeeklyAccessCount: Int? = null,
    val planName: String? = null,
    val expirationDate: Instant? = null
)

@Serializable
data class MemberLoginRequest(
    val email: String,
    val dni: String
)

@Serializable
data class MemberLoginResponse(
    val success: Boolean,
    val memberId: String? = null,
    val name: String? = null,
    val error: String? = null
)

@Serializable
data class Payment(
    val id: String,
    val userId: String,
    val amount: Double,
    val paymentDate: Instant,
    val expirationDate: Instant,
    val method: String
)

@Serializable
data class PaymentRequest(
    val userId: String,
    val amount: Double,
    val method: String
)

@Serializable
data class RiskMember(val name: String, val daysRemaining: Int, val amount: Double)

@Serializable
data class Arrival(val name: String, val plan: String, val timestamp: Instant)

@Serializable
data class MonthlyRevenue(val month: String, val amount: Double)

@Serializable
data class DashboardSummary(
    val totalActiveMembers: Int,
    val totalExpiredMembers: Int,
    val expiringSoonCount: Int,
    val todayCheckInsCount: Int,
    val overdueRisk: List<RiskMember>,
    val recentArrivals: List<Arrival>,
    val revenueFlow: List<MonthlyRevenue>
)
