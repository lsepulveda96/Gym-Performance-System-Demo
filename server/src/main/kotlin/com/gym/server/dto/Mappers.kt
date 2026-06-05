package com.gym.server.dto

import com.gym.server.dto.request.*
import com.gym.server.dto.response.*
import com.gym.shared.domain.*

// Request Mappers
fun LoginRequestDto.toDomain() = LoginRequest(
    email = email,
    password = password,
    role = role
)

fun CreateMemberRequest.toDomain() = MemberRequest(
    name = name,
    email = email,
    dni = dni,
    phone = phone,
    planId = planId,
    isActive = isActive,
    paymentAmount = paymentAmount,
    paymentMethod = paymentMethod
)

fun UpdateMemberRequest.toDomain() = MemberRequest(
    name = name,
    email = email,
    dni = dni,
    phone = phone,
    planId = planId,
    isActive = isActive,
    paymentAmount = 0.0,
    paymentMethod = ""
)

fun CreatePaymentRequest.toDomain() = PaymentRequest(
    userId = userId,
    amount = amount,
    method = method
)

fun AccessValidationRequestDto.toDomain() = AccessValidationRequest(
    code = code
)

fun CreatePlanRequest.toDomain() = GymPlan(
    id = id,
    name = name,
    price = price,
    durationDays = durationDays,
    description = description,
    weeklyLimit = weeklyLimit
)

fun UpdatePlanRequest.toDomain(id: String) = GymPlan(
    id = id,
    name = name,
    price = price,
    durationDays = durationDays,
    description = description,
    weeklyLimit = weeklyLimit
)

// Response Mappers
fun GymUser.toResponse() = GymUserResponse(
    id = id,
    email = email,
    name = name,
    role = role,
    profileImageUrl = profileImageUrl
)

fun AuthResponse.toResponse() = AuthResponseDto(
    token = token,
    user = user.toResponse()
)

fun Member.toResponse() = MemberResponse(
    id = id,
    name = name,
    email = email,
    role = role,
    joinDate = joinDate,
    status = status,
    currentPlan = currentPlan,
    phone = phone,
    dni = dni,
    expirationDate = expirationDate,
    profileImageUrl = profileImageUrl,
    weeklyAttendanceCount = weeklyAttendanceCount,
    weeklyAttendanceLimit = weeklyAttendanceLimit
)

fun GymPlan.toResponse() = PlanResponse(
    id = id,
    name = name,
    price = price,
    durationDays = durationDays,
    description = description,
    weeklyLimit = weeklyLimit
)

fun Payment.toResponse() = PaymentResponse(
    id = id,
    userId = userId,
    amount = amount,
    paymentDate = paymentDate,
    expirationDate = expirationDate,
    method = method
)

fun RiskMember.toResponse() = RiskMemberResponse(
    name = name,
    daysRemaining = daysRemaining,
    amount = amount
)

fun Arrival.toResponse() = ArrivalResponse(
    name = name,
    plan = plan,
    timestamp = timestamp
)

fun MonthlyRevenue.toResponse() = MonthlyRevenueResponse(
    month = month,
    amount = amount
)

fun DashboardSummary.toResponse() = DashboardSummaryResponse(
    totalActiveMembers = totalActiveMembers,
    totalExpiredMembers = totalExpiredMembers,
    expiringSoonCount = expiringSoonCount,
    todayCheckInsCount = todayCheckInsCount,
    overdueRisk = overdueRisk.map { it.toResponse() },
    recentArrivals = recentArrivals.map { it.toResponse() },
    revenueFlow = revenueFlow.map { it.toResponse() }
)

fun AccessValidationResponse.toResponse() = AccessValidationResponseDto(
    success = success,
    message = message,
    memberName = memberName,
    weeklyAccessLimit = weeklyAccessLimit,
    currentWeeklyAccessCount = currentWeeklyAccessCount,
    planName = planName,
    expirationDate = expirationDate
)

fun QRToken.toResponse() = QRTokenResponse(
    token = token,
    expiresAt = expiresAt
)

fun CheckIn.toResponse() = CheckInResponse(
    id = id,
    userId = userId,
    timestamp = timestamp,
    planIdAtTime = planIdAtTime
)
