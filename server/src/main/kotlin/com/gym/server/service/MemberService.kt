package com.gym.server.service

import com.gym.server.repository.*
import com.gym.shared.domain.*
import com.gym.shared.domain.result.Result
import kotlinx.datetime.*
import java.util.UUID

class MemberService(
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
    private val checkInRepository: CheckInRepository,
    private val paymentRepository: PaymentRepository
) {

    suspend fun getMemberDetails(userId: String): Result<Member?> {
        val userResult = userRepository.findUserWithProfile(userId)
        if (userResult is Result.Error) return Result.Error(userResult.message, userResult.cause)
        val userWithProfile = (userResult as Result.Success).data ?: return Result.Success(null)
        val (user, profile, dni) = userWithProfile
        
        val planResult = profile.currentPlanId?.let { planRepository.findById(it) }
        if (planResult is Result.Error) return Result.Error(planResult.message, planResult.cause)
        val plan = planResult?.let { (it as Result.Success).data }
        
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(tz).date
        val daysFromMonday = today.dayOfWeek.ordinal
        val mondayDate = today.minus(daysFromMonday, DateTimeUnit.DAY)
        val mondayStart = LocalDateTime(mondayDate.year, mondayDate.monthNumber, mondayDate.dayOfMonth, 0, 0)
        
        val weeklyCountResult = checkInRepository.getWeeklyCheckInCount(userId, mondayStart.toInstant(tz))
        if (weeklyCountResult is Result.Error) return Result.Error(weeklyCountResult.message, weeklyCountResult.cause)
        val weeklyCount = (weeklyCountResult as Result.Success).data
        
        val latestPaymentResult = paymentRepository.findLatestPaymentForUser(userId)
        if (latestPaymentResult is Result.Error) return Result.Error(latestPaymentResult.message, latestPaymentResult.cause)
        val latestPayment = (latestPaymentResult as Result.Success).data
        
        val isExpired = latestPayment == null || latestPayment.expirationDate < now
        
        return Result.Success(Member(
            id = user.id,
            name = user.name,
            email = user.email,
            role = user.role,
            joinDate = profile.joinDate,
            status = if (isExpired) "Expired" else "Active",
            phone = profile.phone,
            dni = dni,
            currentPlan = plan?.name,
            expirationDate = latestPayment?.expirationDate,
            profileImageUrl = user.profileImageUrl,
            weeklyAttendanceCount = weeklyCount,
            weeklyAttendanceLimit = plan?.weeklyLimit
        ))
    }

    suspend fun getAllMembersDetails(): Result<List<Member>> {
        val usersResult = userRepository.findAllUsersWithProfiles()
        if (usersResult is Result.Error) return Result.Error(usersResult.message, usersResult.cause)
        val usersWithProfiles = (usersResult as Result.Success).data

        val plansResult = planRepository.findAll()
        if (plansResult is Result.Error) return Result.Error(plansResult.message, plansResult.cause)
        val allPlans = (plansResult as Result.Success).data.associateBy { it.id }

        val paymentsResult = paymentRepository.findLatestPaymentsForAllUsers()
        if (paymentsResult is Result.Error) return Result.Error(paymentsResult.message, paymentsResult.cause)
        val latestPayments = (paymentsResult as Result.Success).data
        
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(tz).date
        val daysFromMonday = today.dayOfWeek.ordinal
        val mondayDate = today.minus(daysFromMonday, DateTimeUnit.DAY)
        val mondayStart = LocalDateTime(mondayDate.year, mondayDate.monthNumber, mondayDate.dayOfMonth, 0, 0)
        
        val checkInsResult = checkInRepository.getCheckInsSince(mondayStart.toInstant(tz))
        if (checkInsResult is Result.Error) return Result.Error(checkInsResult.message, checkInsResult.cause)
        val weeklyCheckIns = (checkInsResult as Result.Success).data
            .groupBy { it.userId }
            .mapValues { it.value.size }
            
        val members = usersWithProfiles.map { (user, profile, dni) ->
            val plan = profile.currentPlanId?.let { allPlans[it] }
            val weeklyCount = weeklyCheckIns[user.id] ?: 0
            val latestPayment = latestPayments[user.id]
            val isExpired = latestPayment == null || latestPayment.expirationDate < now
            
            Member(
                id = user.id,
                name = user.name,
                email = user.email,
                role = user.role,
                joinDate = profile.joinDate,
                status = if (isExpired) "Expired" else "Active",
                phone = profile.phone,
                dni = dni,
                currentPlan = plan?.name,
                expirationDate = latestPayment?.expirationDate,
                profileImageUrl = user.profileImageUrl,
                weeklyAttendanceCount = weeklyCount,
                weeklyAttendanceLimit = plan?.weeklyLimit
            )
        }
        return Result.Success(members)
    }

    suspend fun createMember(request: MemberRequest): Result<Unit> {
        val existingEmailResult = userRepository.findUserByEmail(request.email)
        if (existingEmailResult is Result.Error) return Result.Error(existingEmailResult.message, existingEmailResult.cause)
        val existingEmail = (existingEmailResult as Result.Success).data
        if (existingEmail != null) return Result.Error("This Email is already registered in our system")
        
        val newId = "u-${UUID.randomUUID()}"
        val now = Clock.System.now()
        
        val user = GymUser(
            id = newId,
            email = request.email,
            name = request.name,
            role = UserRole.MEMBER
        )
        
        val createResult = userRepository.createUser(user, "default_pass")
        if (createResult is Result.Error) return Result.Error(createResult.message, createResult.cause)
        
        val profile = MemberProfile(
            userId = newId,
            phone = request.phone,
            joinDate = now,
            isActive = request.isActive,
            currentPlanId = if (request.planId.isNullOrEmpty()) null else request.planId
        )
        val createProfileResult = userRepository.createMemberProfile(profile, request.dni)
        if (createProfileResult is Result.Error) {
            val errorMessageText = createProfileResult.message
            val isDuplicateDni = errorMessageText.contains("member_profiles_dni_unique", ignoreCase = true) || 
                                 (createProfileResult.cause?.message?.contains("member_profiles_dni_unique", ignoreCase = true) == true)
            if (isDuplicateDni) {
                return Result.Error("This DNI is already registered in our system")
            }
            return Result.Error(createProfileResult.message, createProfileResult.cause)
        }
        
        val tz = TimeZone.currentSystemDefault()
        val expirationDate = now.plus(30, DateTimeUnit.DAY, tz)
        val payment = Payment(
            id = UUID.randomUUID().toString(),
            userId = newId,
            amount = request.paymentAmount,
            paymentDate = now,
            expirationDate = expirationDate,
            method = request.paymentMethod
        )
        val createPaymentResult = paymentRepository.create(payment)
        if (createPaymentResult is Result.Error) return Result.Error(createPaymentResult.message, createPaymentResult.cause)
        
        return Result.Success(Unit)
    }

    suspend fun updateMember(id: String, request: MemberRequest): Result<Unit> {
        val updateUserResult = userRepository.updateUser(id, request.email, request.name)
        if (updateUserResult is Result.Error) return Result.Error(updateUserResult.message, updateUserResult.cause)

        val planId = if (request.planId.isNullOrEmpty()) null else request.planId
        val updateProfileResult = userRepository.updateMemberProfile(id, request.dni, request.phone, planId)
        if (updateProfileResult is Result.Error) return Result.Error(updateProfileResult.message, updateProfileResult.cause)

        return Result.Success(Unit)
    }

    suspend fun updateProfileImage(userId: String, imageUrl: String): Result<Unit> {
        return userRepository.updateProfileImage(userId, imageUrl)
    }
}
