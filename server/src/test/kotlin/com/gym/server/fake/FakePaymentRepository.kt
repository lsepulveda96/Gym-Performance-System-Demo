package com.gym.server.fake

import com.gym.server.repository.PaymentRepository
import com.gym.shared.domain.Payment
import com.gym.shared.domain.result.Result
import kotlinx.datetime.Instant

/**
 * In-memory [PaymentRepository] for deterministic payment and access tests.
 */
class FakePaymentRepository : PaymentRepository {
    val payments = mutableListOf<Payment>()
    var failOnCreate: Result<Unit>? = null

    fun seed(payment: Payment) {
        payments.add(payment)
    }

    fun latestForUser(userId: String): Payment? =
        payments.filter { it.userId == userId }.maxByOrNull { it.paymentDate }

    override suspend fun create(payment: Payment): Result<Unit> {
        failOnCreate?.let { if (it is Result.Error) return it }
        payments.add(payment)
        return Result.Success(Unit)
    }

    override suspend fun findByUserIdOrderByDateDesc(userId: String): Result<List<Payment>> =
        Result.Success(
            payments.filter { it.userId == userId }.sortedByDescending { it.paymentDate },
        )

    override suspend fun findLatestPaymentForUser(userId: String): Result<Payment?> =
        Result.Success(latestForUser(userId))

    override suspend fun findLatestPaymentsForAllUsers(): Result<Map<String, Payment>> =
        Result.Success(
            payments.groupBy { it.userId }.mapValues { (_, list) -> list.maxBy { it.paymentDate } },
        )

    override suspend fun getPaymentsInDateRange(startDate: Instant, endDate: Instant): Result<List<Payment>> =
        Result.Success(payments.filter { it.paymentDate in startDate..endDate })
}
