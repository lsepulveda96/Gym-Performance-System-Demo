package com.gym.server.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import com.gym.server.service.AuthService
import com.gym.server.dto.request.LoginRequestDto
import com.gym.server.dto.toDomain
import com.gym.server.dto.toResponse
import com.gym.shared.domain.result.Result

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/login") {
            val request = call.receive<LoginRequestDto>()
            val authResult = authService.login(request.toDomain())

            when (authResult) {
                is Result.Success -> call.respond(authResult.data.toResponse())
                is Result.Error -> call.respond(HttpStatusCode.Unauthorized, authResult.message)
            }
        }
        
        post("/register") {
            // Placeholder logic
            call.respond(HttpStatusCode.Created)
        }
    }
}
