package com.gym.server.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.http.*
import com.gym.server.service.AccessService
import com.gym.server.dto.request.AccessValidationRequestDto
import com.gym.server.dto.toDomain
import com.gym.server.dto.toResponse
import com.gym.shared.domain.result.Result
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.minutes

fun Route.accessRoutes(accessService: AccessService) {
    route("/access") {
        post("/validate") {
            val request = call.receive<AccessValidationRequestDto>()
            val result = accessService.validateAccess(request.code.trim())
            call.respondResult(result) { it.toResponse() }
        }

        authenticate("auth-jwt", "auth-simple") {
            post("/generate") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "User not identified")
                    return@post
                }

                val timestamp = Clock.System.now().epochSeconds
                val signature = AccessService.generateSignature(userId, timestamp)
                val token = "gym:access:${userId}:${timestamp}:${signature}"
                val expiresAt = Clock.System.now().plus(5.minutes)
                val qrToken = com.gym.shared.domain.QRToken(token, expiresAt)

                call.respond(qrToken.toResponse())
            }

            get("/member/{memberId}") {
                val memberId = call.parameters["memberId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val result = accessService.getMemberCheckIns(memberId)
                call.respondResult(result) { logs -> logs.map { it.toResponse() } }
            }
        }
    }
}
