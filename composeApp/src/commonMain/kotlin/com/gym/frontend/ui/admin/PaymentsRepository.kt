package com.gym.frontend.ui.admin

import com.gym.shared.domain.Payment
import com.gym.shared.domain.PaymentRequest

class PaymentsRepository(private val service: PaymentsService) {
    suspend fun getMemberPayments(memberId: String): Result<List<Payment>> {
        return service.getMemberPayments(memberId)
    }

    suspend fun createPayment(request: PaymentRequest): Result<String> {
        return service.createPayment(request)
    }
}
