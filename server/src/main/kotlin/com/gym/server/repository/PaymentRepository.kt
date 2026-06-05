package com.gym.server.repository

import com.gym.shared.domain.Payment
import kotlinx.datetime.Instant
import com.gym.shared.domain.result.Result

interface PaymentRepository {
    suspend fun create(payment: Payment): Result<Unit>
    suspend fun findByUserIdOrderByDateDesc(userId: String): Result<List<Payment>>
    suspend fun findLatestPaymentForUser(userId: String): Result<Payment?>
    suspend fun findLatestPaymentsForAllUsers(): Result<Map<String, Payment>>
    suspend fun getPaymentsInDateRange(startDate: Instant, endDate: Instant): Result<List<Payment>>
}
