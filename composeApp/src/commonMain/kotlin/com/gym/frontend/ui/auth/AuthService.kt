package com.gym.frontend.ui.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.gym.shared.domain.*
import com.gym.frontend.ui.config.AppConfig

class AuthService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
        }
        defaultRequest {
            url(AppConfig.apiBaseUrl())
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response = client.post("/auth/login") {
                setBody(request)
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Login failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMe(token: String): Result<Member> {
        return try {
            val response = client.get("/members/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch profile: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfileImage(token: String, imageUrl: String): Result<Unit> {
        return try {
            val response = client.put("/members/me/profile-image") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(mapOf("profileImageUrl" to imageUrl))
            }
            if (response.status == HttpStatusCode.OK) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update profile image: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
