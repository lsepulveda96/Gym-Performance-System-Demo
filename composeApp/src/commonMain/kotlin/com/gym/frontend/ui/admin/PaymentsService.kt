package com.gym.frontend.ui.admin

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.gym.shared.domain.*
import com.gym.frontend.ui.config.AppConfig
import com.gym.frontend.ui.demo.DemoData

class PaymentsService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        defaultRequest {
            url(AppConfig.apiBaseUrl())
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun getMemberPayments(memberId: String): Result<List<Payment>> {
        if (AppConfig.demoMode) return Result.success(DemoData.payments.filter { it.userId == memberId }.ifEmpty { DemoData.payments })
        return try {
            val response = client.get("/payments/member/$memberId")
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch payments: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPayment(request: PaymentRequest): Result<String> {
        if (AppConfig.demoMode) return Result.success("demo-payment-id")
        return try {
            val response = client.post("/payments") {
                setBody(request)
            }
            if (response.status == HttpStatusCode.Created) {
                val map = response.body<Map<String, String>>()
                Result.success(map["id"] ?: "")
            } else {
                Result.failure(Exception("Failed to create payment: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
