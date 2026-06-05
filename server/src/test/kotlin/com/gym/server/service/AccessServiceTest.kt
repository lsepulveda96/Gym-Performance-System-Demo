package com.gym.server.service

import com.gym.server.fake.FakeCheckInRepository
import com.gym.server.fake.FakeMemberRepository
import com.gym.server.fake.FakePaymentRepository
import com.gym.server.fake.FakePlanRepository
import com.gym.server.repository.TestFixtures
import com.gym.shared.domain.result.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName

@DisplayName("AccessService business rules")
class AccessServiceTest {

    private lateinit var memberRepository: FakeMemberRepository
    private lateinit var planRepository: FakePlanRepository
    private lateinit var checkInRepository: FakeCheckInRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var accessService: AccessService

    @BeforeEach
    fun setUp() {
        memberRepository = FakeMemberRepository()
        planRepository = FakePlanRepository()
        checkInRepository = FakeCheckInRepository()
        paymentRepository = FakePaymentRepository()
        accessService = AccessService(
            userRepository = memberRepository,
            planRepository = planRepository,
            checkInRepository = checkInRepository,
            paymentRepository = paymentRepository,
        )
    }

    private fun seedActiveMember(weeklyCount: Int = 0, weeklyLimit: Int? = 3) {
        val user = TestFixtures.gymUser()
        val profile = TestFixtures.memberProfile()
        val plan = TestFixtures.gymPlan(weeklyLimit = weeklyLimit)
        memberRepository.seedMember(user, profile)
        planRepository.seed(plan)
        paymentRepository.seed(TestFixtures.activePayment())
        checkInRepository.weeklyCountOverride = weeklyCount
    }

    @Test
    fun `active member with valid QR can enter`() = runTest {
        seedActiveMember(weeklyCount = 1, weeklyLimit = 3)

        val result = accessService.validateAccess(TestFixtures.validAccessQr())

        assertIs<Result.Success<*>>(result)
        val response = (result as Result.Success).data
        assertTrue(response.success)
        assertEquals("Access granted", response.message)
        assertEquals("Test Member", response.memberName)
        assertEquals(2, response.currentWeeklyAccessCount)
        assertEquals(1, checkInRepository.checkIns.size)
    }

    @Test
    fun `expired member cannot enter`() = runTest {
        seedActiveMember()
        paymentRepository.payments.clear()
        paymentRepository.seed(TestFixtures.expiredPayment())

        val result = accessService.validateAccess(TestFixtures.validAccessQr())

        assertIs<Result.Success<*>>(result)
        val response = (result as Result.Success).data
        assertFalse(response.success)
        assertEquals("Membership expired", response.message)
        assertEquals(0, checkInRepository.checkIns.size)
    }

    @Test
    fun `member at weekly limit cannot enter`() = runTest {
        seedActiveMember(weeklyCount = 3, weeklyLimit = 3)

        val result = accessService.validateAccess(TestFixtures.validAccessQr())

        assertIs<Result.Success<*>>(result)
        val response = (result as Result.Success).data
        assertFalse(response.success)
        assertEquals("Weekly limit reached", response.message)
        assertEquals(3, response.currentWeeklyAccessCount)
        assertEquals(3, response.weeklyAccessLimit)
        assertEquals(0, checkInRepository.checkIns.size)
    }

    @Test
    fun `member under weekly limit can enter`() = runTest {
        seedActiveMember(weeklyCount = 2, weeklyLimit = 3)

        val result = accessService.validateAccess(TestFixtures.validAccessQr())

        assertIs<Result.Success<*>>(result)
        val response = (result as Result.Success).data
        assertTrue(response.success)
        assertEquals("Access granted", response.message)
        assertEquals(3, response.currentWeeklyAccessCount)
        assertEquals(1, checkInRepository.checkIns.size)
    }
}
