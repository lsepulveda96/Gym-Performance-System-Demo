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

class AdminService(private val token: String? = null) {
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
            if (token != null) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    suspend fun getPlans(): List<GymPlan> {
        if (AppConfig.demoMode) return DemoData.plans
        return try {
            client.get("/admin/plans").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createPlan(plan: GymPlan) {
        if (AppConfig.demoMode) return
        val response = client.post("/admin/plans") {
            setBody(plan)
        }
        if (response.status != HttpStatusCode.Created) {
            throw Exception("Failed to create plan")
        }
    }

    suspend fun updatePlan(id: String, plan: GymPlan) {
        if (AppConfig.demoMode) return
        val response = client.put("/admin/plans/$id") {
            setBody(plan)
        }
        if (response.status != HttpStatusCode.OK) {
            throw Exception("Failed to update plan")
        }
    }
}

class AdminRepository(private val service: AdminService) {
    suspend fun getPlans(): Result<List<GymPlan>> {
        return try {
            Result.success(service.getPlans())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPlan(plan: GymPlan): Result<Boolean> {
        return try {
            service.createPlan(plan)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePlan(id: String, plan: GymPlan): Result<Boolean> {
        return try {
            service.updatePlan(id, plan)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
