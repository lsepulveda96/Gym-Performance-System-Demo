package com.gym.server.service

import com.gym.server.repository.*
import com.gym.shared.domain.*
import com.gym.shared.domain.result.Result
import kotlinx.datetime.*
import java.util.UUID

class AccessService(
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
    private val checkInRepository: CheckInRepository,
    private val paymentRepository: PaymentRepository
) {
    companion object {
        private const val QR_SECRET = "kinetic-gym-super-secret-2026"
        private const val GRANTED = "Access granted"
        private const val EXPIRED = "Membership expired"
        private const val WEEKLY_LIMIT = "Weekly limit reached"
        private const val INVALID_QR = "Invalid QR"
        private const val QR_EXPIRED = "QR expired"
        private const val MEMBER_NOT_FOUND = "Member not found"
        private const val TAMPERED = "Invalid QR"

        fun generateSignature(memberId: String, timestamp: Long): String {
            val input = memberId + timestamp + QR_SECRET
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray())
            return digest.fold("") { str, it -> str + "%02x".format(it) }.take(16)
        }
    }

    suspend fun validateAccess(code: String): Result<AccessValidationResponse> {
        val parts = code.split(":")
        if (parts.size != 5 || parts[0] != "gym" || parts[1] != "access") {
            return Result.Success(denied(INVALID_QR))
        }

        val memberId = parts[2]
        val timestampEpoch = parts[3].toLongOrNull() ?: 0L
        val signature = parts[4]
        val timestamp = Instant.fromEpochSeconds(timestampEpoch)

        val expectedSignature = generateSignature(memberId, timestampEpoch)
        if (signature != expectedSignature) {
            return Result.Success(denied(TAMPERED))
        }

        val now = Clock.System.now()
        val diff = now.epochSeconds - timestamp.epochSeconds
        if (diff > 300 || diff < -60) {
            return Result.Success(denied(QR_EXPIRED))
        }

        val userResult = userRepository.findUserWithProfile(memberId)
        if (userResult is Result.Error) return Result.Error(userResult.message, userResult.cause)
        val userWithProfile = (userResult as Result.Success).data
        if (userWithProfile == null) {
            return Result.Success(denied(MEMBER_NOT_FOUND))
        }
        val (user, profile, _) = userWithProfile
        
        val tz = TimeZone.currentSystemDefault()
        val nowLocal = now.toLocalDateTime(tz)
        
        val latestPaymentResult = paymentRepository.findLatestPaymentForUser(memberId)
        if (latestPaymentResult is Result.Error) return Result.Error(latestPaymentResult.message, latestPaymentResult.cause)
        val latestPayment = (latestPaymentResult as Result.Success).data

        val expirationDate = latestPayment?.expirationDate
        val isExpired = expirationDate == null || expirationDate.toLocalDateTime(tz) < nowLocal

        val today = nowLocal.date
        val daysFromMonday = today.dayOfWeek.ordinal
        val mondayDate = today.minus(daysFromMonday, DateTimeUnit.DAY)
        val mondayStart = LocalDateTime(mondayDate.year, mondayDate.monthNumber, mondayDate.dayOfMonth, 0, 0)

        val weeklyCountResult = checkInRepository.getWeeklyCheckInCount(memberId, mondayStart.toInstant(tz))
        if (weeklyCountResult is Result.Error) return Result.Error(weeklyCountResult.message, weeklyCountResult.cause)
        val weeklyCount = (weeklyCountResult as Result.Success).data

        val planId = profile.currentPlanId
        val planResult = if (planId != null) planRepository.findById(planId) else null
        if (planResult is Result.Error) return Result.Error(planResult.message, planResult.cause)
        val plan = if (planResult != null) (planResult as Result.Success).data else null
        
        val planName = plan?.name ?: "No Plan"
        val weeklyLimit = plan?.weeklyLimit

        val respBase = AccessValidationResponse(
            success = false,
            message = "",
            memberName = user.name,
            weeklyAccessLimit = weeklyLimit,
            currentWeeklyAccessCount = weeklyCount,
            planName = planName,
            expirationDate = expirationDate
        )

        if (isExpired) {
            return Result.Success(respBase.copy(message = EXPIRED))
        }

        if (weeklyLimit != null && weeklyCount >= weeklyLimit) {
            return Result.Success(respBase.copy(message = WEEKLY_LIMIT))
        }

        val checkIn = CheckIn(
            id = UUID.randomUUID().toString(),
            userId = memberId,
            timestamp = now,
            planIdAtTime = planId ?: "p-basic"
        )
        val createCheckInResult = checkInRepository.create(checkIn)
        if (createCheckInResult is Result.Error) return Result.Error(createCheckInResult.message, createCheckInResult.cause)

        return Result.Success(respBase.copy(
            success = true,
            message = GRANTED,
            currentWeeklyAccessCount = weeklyCount + 1
        ))
    }

    suspend fun getMemberCheckIns(memberId: String): Result<List<CheckIn>> {
        return checkInRepository.getCheckInsByUserId(memberId)
    }

    private fun denied(message: String, memberName: String? = null) =
        AccessValidationResponse(success = false, message = message, memberName = memberName)
}
