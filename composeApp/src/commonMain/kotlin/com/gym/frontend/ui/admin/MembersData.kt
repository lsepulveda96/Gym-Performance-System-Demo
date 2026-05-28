package com.gym.frontend.ui.admin

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.gym.shared.domain.*
import com.gym.frontend.ui.auth.TokenManager
import kotlinx.serialization.Serializable
import com.gym.frontend.ui.config.AppConfig
import com.gym.frontend.ui.demo.DemoData

@Serializable
data class ErrorResponse(val error: String)

class MembersService(private val tokenManager: TokenManager = TokenManager()) {
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

    suspend fun getMembers(): List<Member> {
        if (AppConfig.demoMode) return DemoData.members
        return try {
            client.get("/admin/members") {
                val jwt = tokenManager.getToken()
                if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
            }.body()
        } catch (e: Throwable) {
            emptyList()
        }
    }

    suspend fun getPlans(): List<GymPlan> {
        if (AppConfig.demoMode) return DemoData.plans
        return try {
            client.get("/admin/plans") {
                val jwt = tokenManager.getToken()
                if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
            }.body()
        } catch (e: Throwable) {
            emptyList()
        }
    }

    suspend fun createMember(request: MemberRequest) {
        if (AppConfig.demoMode) return   // demo: silently succeed
        val response = client.post("/admin/members") {
            val jwt = tokenManager.getToken()
            if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
            setBody(request)
        }
        if (response.status != HttpStatusCode.Created) {
            val errorBody = try { response.body<ErrorResponse>().error } catch (e: Throwable) { "Unknown error" }
            throw Exception(errorBody)
        }
    }

    suspend fun updateMember(id: String, request: MemberRequest) {
        if (AppConfig.demoMode) return   // demo: silently succeed
        val response = client.put("/admin/members/$id") {
            val jwt = tokenManager.getToken()
            if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
            setBody(request)
        }
        if (response.status != HttpStatusCode.OK) {
            val errorBody = try { response.body<ErrorResponse>().error } catch (e: Throwable) { "Unknown error" }
            throw Exception(errorBody)
        }
    }

    suspend fun getAttendanceHistory(memberId: String): List<CheckIn> {
        if (AppConfig.demoMode) return DemoData.checkIns.filter { it.userId == memberId }.ifEmpty { DemoData.checkIns }
        return try {
            client.get("/access/member/$memberId") {
                val jwt = tokenManager.getToken()
                if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
            }.body()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class MembersRepository(private val service: MembersService) {
    suspend fun getMembers(): Result<List<Member>> {
        return try {
            Result.success(service.getMembers())
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun getPlans(): Result<List<GymPlan>> {
        return try {
            Result.success(service.getPlans())
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun createMember(request: MemberRequest): Result<Boolean> {
        return try {
            service.createMember(request)
            Result.success(true)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun updateMember(id: String, request: MemberRequest): Result<Boolean> {
        return try {
            service.updateMember(id, request)
            Result.success(true)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceHistory(memberId: String): Result<List<CheckIn>> {
        return try {
            Result.success(service.getAttendanceHistory(memberId))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
