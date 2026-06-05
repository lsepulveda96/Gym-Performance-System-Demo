package com.gym.server.repository

import com.gym.server.service.AccessService
import com.gym.shared.domain.GymPlan
import com.gym.shared.domain.GymUser
import com.gym.shared.domain.MemberProfile
import com.gym.shared.domain.MemberRequest
import com.gym.shared.domain.Payment
import com.gym.shared.domain.UserRole
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.minus

object TestFixtures {
    const val MEMBER_ID = "u-test-member"
    const val PLAN_ID = "plan-test-basic"

    fun gymUser(
        id: String = MEMBER_ID,
        email: String = "member@test.com",
        name: String = "Test Member",
    ): GymUser = GymUser(
        id = id,
        email = email,
        name = name,
        role = UserRole.MEMBER,
    )

    fun memberProfile(
        userId: String = MEMBER_ID,
        planId: String? = PLAN_ID,
    ): MemberProfile {
        val now = Clock.System.now()
        return MemberProfile(
            userId = userId,
            phone = "555-0100",
            joinDate = now,
            isActive = true,
            currentPlanId = planId,
        )
    }

    fun gymPlan(
        id: String = PLAN_ID,
        name: String = "Basic",
        weeklyLimit: Int? = 3,
    ): GymPlan = GymPlan(
        id = id,
        name = name,
        price = 29.99,
        durationDays = 30,
        weeklyLimit = weeklyLimit,
    )

    fun activePayment(userId: String = MEMBER_ID): Payment {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        return Payment(
            id = "pay-active",
            userId = userId,
            amount = 50.0,
            paymentDate = now,
            expirationDate = now.plus(30, DateTimeUnit.DAY, tz),
            method = "cash",
        )
    }

    fun expiredPayment(userId: String = MEMBER_ID): Payment {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val past = now.minus(1, DateTimeUnit.DAY, tz)
        return Payment(
            id = "pay-expired",
            userId = userId,
            amount = 50.0,
            paymentDate = past.minus(30, DateTimeUnit.DAY, tz),
            expirationDate = past,
            method = "cash",
        )
    }

    fun memberRequest(
        email: String = "new-member@test.com",
        dni: String = "11223344",
        planId: String? = PLAN_ID,
    ): MemberRequest = MemberRequest(
        name = "New Member",
        email = email,
        dni = dni,
        phone = "555-9999",
        planId = planId,
        isActive = true,
        paymentAmount = 45.0,
        paymentMethod = "cash",
    )

    /** Valid QR code for [AccessService.validateAccess] at the current instant. */
    fun validAccessQr(memberId: String = MEMBER_ID, at: Instant = Clock.System.now()): String {
        val epoch = at.epochSeconds
        val signature = AccessService.generateSignature(memberId, epoch)
        return "gym:access:$memberId:$epoch:$signature"
    }
}
