package com.gym.server.service

import com.gym.server.fake.FakeCheckInRepository
import com.gym.server.fake.FakeMemberRepository
import com.gym.server.fake.FakePaymentRepository
import com.gym.server.fake.FakePlanRepository
import com.gym.server.repository.TestFixtures
import com.gym.shared.domain.MemberRequest
import com.gym.shared.domain.result.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName

@DisplayName("MemberService business rules")
class MemberServiceTest {

    private lateinit var memberRepository: FakeMemberRepository
    private lateinit var planRepository: FakePlanRepository
    private lateinit var checkInRepository: FakeCheckInRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var memberService: MemberService

    @BeforeEach
    fun setUp() {
        memberRepository = FakeMemberRepository()
        planRepository = FakePlanRepository()
        checkInRepository = FakeCheckInRepository()
        paymentRepository = FakePaymentRepository()
        memberService = MemberService(
            userRepository = memberRepository,
            planRepository = planRepository,
            checkInRepository = checkInRepository,
            paymentRepository = paymentRepository,
        )
    }

    @Test
    fun `createMember registers user profile and initial payment`() = runTest {
        planRepository.seed(TestFixtures.gymPlan())
        val request = TestFixtures.memberRequest(email = "brand-new@test.com", dni = "55667788")

        val result = memberService.createMember(request)

        assertIs<Result.Success<*>>(result)
        assertEquals(1, memberRepository.users.size)
        assertEquals(1, memberRepository.profiles.size)
        assertEquals(1, paymentRepository.payments.size)
        val createdUser = memberRepository.users.values.single()
        assertEquals("brand-new@test.com", createdUser.email)
        assertEquals("New Member", createdUser.name)
        val payment = paymentRepository.payments.single()
        assertEquals(45.0, payment.amount)
        assertEquals("cash", payment.method)
        val tz = TimeZone.currentSystemDefault()
        val expectedExpiration = payment.paymentDate.plus(30, DateTimeUnit.DAY, tz)
        assertEquals(expectedExpiration, payment.expirationDate)
    }

    @Test
    fun `updateMember updates user and profile data`() = runTest {
        val user = TestFixtures.gymUser()
        val profile = TestFixtures.memberProfile()
        memberRepository.seedMember(user, profile, dni = "12345678")
        planRepository.seed(TestFixtures.gymPlan())

        val result = memberService.updateMember(
            TestFixtures.MEMBER_ID,
            MemberRequest(
                name = "Updated Name",
                email = "updated@test.com",
                dni = "87654321",
                phone = "555-0001",
                planId = TestFixtures.PLAN_ID,
                paymentAmount = 0.0,
                paymentMethod = "cash",
            ),
        )

        assertIs<Result.Success<*>>(result)
        val updatedUser = memberRepository.users[TestFixtures.MEMBER_ID]!!
        val updatedProfile = memberRepository.profiles[TestFixtures.MEMBER_ID]!!
        assertEquals("Updated Name", updatedUser.name)
        assertEquals("updated@test.com", updatedUser.email)
        assertEquals("555-0001", updatedProfile.phone)
    }

    @Test
    fun `createMember rejects duplicate email`() = runTest {
        memberRepository.seedMember(TestFixtures.gymUser(), TestFixtures.memberProfile())

        val result = memberService.createMember(
            TestFixtures.memberRequest(email = "member@test.com", dni = "99999999"),
        )

        assertIs<Result.Error>(result)
        assertEquals("This Email is already registered in our system", result.message)
        assertEquals(1, memberRepository.users.size)
        assertEquals(0, paymentRepository.payments.size)
    }

    @Test
    fun `createMember rejects duplicate DNI`() = runTest {
        memberRepository.seedMember(
            TestFixtures.gymUser(),
            TestFixtures.memberProfile(),
            dni = "11223344",
        )

        val result = memberService.createMember(
            TestFixtures.memberRequest(email = "other@test.com", dni = "11223344"),
        )

        assertIs<Result.Error>(result)
        assertEquals("This DNI is already registered in our system", result.message)
        assertEquals(0, paymentRepository.payments.size)
        // User row is created before profile; duplicate DNI fails at profile step (no rollback).
        assertEquals(2, memberRepository.users.size)
        assertEquals(1, memberRepository.profiles.size)
    }

    @Test
    fun `getMemberDetails marks member Expired when payment is past due`() = runTest {
        val user = TestFixtures.gymUser()
        val profile = TestFixtures.memberProfile()
        memberRepository.seedMember(user, profile)
        planRepository.seed(TestFixtures.gymPlan())
        paymentRepository.seed(TestFixtures.expiredPayment())
        checkInRepository.weeklyCountOverride = 0

        val result = memberService.getMemberDetails(TestFixtures.MEMBER_ID)

        assertIs<Result.Success<*>>(result)
        val member = (result as Result.Success).data
        assertEquals("Expired", member?.status)
    }

    @Test
    fun `getMemberDetails marks member Active with valid payment`() = runTest {
        val user = TestFixtures.gymUser()
        val profile = TestFixtures.memberProfile()
        memberRepository.seedMember(user, profile)
        planRepository.seed(TestFixtures.gymPlan())
        paymentRepository.seed(TestFixtures.activePayment())
        checkInRepository.weeklyCountOverride = 1

        val result = memberService.getMemberDetails(TestFixtures.MEMBER_ID)

        assertIs<Result.Success<*>>(result)
        val member = (result as Result.Success).data
        assertEquals("Active", member?.status)
        assertEquals("Basic", member?.currentPlan)
        assertEquals(1, member?.weeklyAttendanceCount)
    }
}
