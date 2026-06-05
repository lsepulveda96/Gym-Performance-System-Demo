package com.gym.server.service

import com.gym.server.fake.FakePaymentRepository
import com.gym.server.repository.TestFixtures
import com.gym.shared.domain.PaymentRequest
import com.gym.shared.domain.result.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName

@DisplayName("PaymentService business rules")
class PaymentServiceTest {

    @Test
    fun `payment is registered correctly in fake repository`() = runTest {
        val paymentRepository = FakePaymentRepository()
        val service = PaymentService(paymentRepository)
        val before = Clock.System.now()

        val result = service.createPayment(
            PaymentRequest(
                userId = TestFixtures.MEMBER_ID,
                amount = 80.0,
                method = "card",
            ),
        )

        assertIs<Result.Success<String>>(result)
        assertEquals(1, paymentRepository.payments.size)
        val stored = paymentRepository.payments.single()
        assertEquals(TestFixtures.MEMBER_ID, stored.userId)
        assertEquals(80.0, stored.amount)
        assertEquals("card", stored.method)
        assertEquals(result.data, stored.id)
        assertTrue(stored.paymentDate.epochSeconds >= before.epochSeconds)
    }

    @Test
    fun `expiration date is calculated as 30 days from payment date`() = runTest {
        val paymentRepository = FakePaymentRepository()
        val service = PaymentService(paymentRepository)
        val tz = TimeZone.currentSystemDefault()
        val before = Clock.System.now()

        service.createPayment(
            PaymentRequest(userId = TestFixtures.MEMBER_ID, amount = 50.0, method = "cash"),
        )

        val stored = paymentRepository.payments.single()
        val expectedExpiration = stored.paymentDate.plus(30, DateTimeUnit.DAY, tz)
        assertEquals(expectedExpiration, stored.expirationDate)
        assertTrue(stored.expirationDate.epochSeconds >= before.plus(30, DateTimeUnit.DAY, tz).epochSeconds - 2)
    }

    @Test
    fun `getMemberHistory returns payments ordered by date descending`() = runTest {
        val paymentRepository = FakePaymentRepository()
        val older = TestFixtures.expiredPayment()
        val newer = TestFixtures.activePayment().copy(id = "pay-newer")
        paymentRepository.seed(older)
        paymentRepository.seed(newer)
        val service = PaymentService(paymentRepository)

        val result = service.getMemberHistory(TestFixtures.MEMBER_ID)

        assertIs<Result.Success<*>>(result)
        val history = (result as Result.Success).data
        assertEquals(2, history.size)
        assertEquals("pay-newer", history.first().id)
    }
}
