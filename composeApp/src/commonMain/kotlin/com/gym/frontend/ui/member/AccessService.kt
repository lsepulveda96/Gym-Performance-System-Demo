package com.gym.frontend.ui.member

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.gym.shared.domain.QRToken
import com.gym.frontend.ui.auth.TokenManager
import com.gym.frontend.ui.config.AppConfig
import com.gym.frontend.ui.demo.DemoData

class AccessService(private val tokenManager: TokenManager) {
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

    suspend fun generateAccessCode(): Result<QRToken> {
        if (AppConfig.demoMode) return Result.success(DemoData.demoQrToken())
        return try {
            val jwt = tokenManager.getToken() ?: return Result.failure(Exception("No token found"))
            val response = client.post("/access/generate") {
                header(HttpHeaders.Authorization, "Bearer $jwt")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Server returned ${response.status}"))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun validateAccess(code: String): Result<com.gym.shared.domain.AccessValidationResponse> {
        if (AppConfig.demoMode) {
            // Simulate scan result: expired token hint → denied, everything else → granted
            val result = if (code.contains("EXPIRED") || code.contains("expired"))
                DemoData.demoValidationExpired
            else
                DemoData.demoValidationGranted
            return Result.success(result)
        }
        return try {
            val response = client.post("/access/validate") {
                tokenManager.getToken()?.let { jwt ->
                    header(HttpHeaders.Authorization, "Bearer $jwt")
                }
                setBody(com.gym.shared.domain.AccessValidationRequest(code.trim()))
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Server returned ${response.status}"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
}
