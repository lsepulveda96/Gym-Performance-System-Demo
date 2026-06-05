package com.gym.server.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import com.gym.server.service.PaymentService
import com.gym.server.dto.request.CreatePaymentRequest
import com.gym.server.dto.toDomain
import com.gym.server.dto.toResponse
import com.gym.shared.domain.result.Result

fun Route.paymentRoutes(paymentService: PaymentService) {
    route("/payments") {
        post {
            val request = call.receive<CreatePaymentRequest>()
            val result = paymentService.createPayment(request.toDomain())
            call.respondResultWithStatus(result, HttpStatusCode.Created) { mapOf("id" to it) }
        }

        get("/member/{memberId}") {
            val memberId = call.parameters["memberId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val result = paymentService.getMemberHistory(memberId)
            call.respondResult(result) { history -> history.map { it.toResponse() } }
        }
    }
}
