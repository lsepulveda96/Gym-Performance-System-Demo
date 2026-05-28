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

class DashboardService {
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

    suspend fun getDashboardSummary(): Result<DashboardSummary> {
        if (AppConfig.demoMode) return Result.success(DemoData.dashboardSummary)
        return try {
            val response = client.get("/dashboard/summary")
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch dashboard: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
