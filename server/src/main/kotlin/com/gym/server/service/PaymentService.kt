package com.gym.server.service

import com.gym.server.repository.PaymentRepository
import com.gym.shared.domain.Payment
import com.gym.shared.domain.PaymentRequest
import com.gym.shared.domain.result.Result
import kotlinx.datetime.*
import java.util.UUID

class PaymentService(private val paymentRepository: PaymentRepository) {
    suspend fun createPayment(request: PaymentRequest): Result<String> {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val expirationDate = now.plus(30, DateTimeUnit.DAY, tz)
        
        val newId = UUID.randomUUID().toString()
        val payment = Payment(
            id = newId,
            userId = request.userId,
            amount = request.amount,
            paymentDate = now,
            expirationDate = expirationDate,
            method = request.method
        )
        val createResult = paymentRepository.create(payment)
        if (createResult is Result.Error) {
            return Result.Error(createResult.message, createResult.cause)
        }
        return Result.Success(newId)
    }
    
    suspend fun getMemberHistory(memberId: String): Result<List<Payment>> {
        return paymentRepository.findByUserIdOrderByDateDesc(memberId)
    }
}
