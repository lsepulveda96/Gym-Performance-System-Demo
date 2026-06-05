package com.gym.server.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.HttpStatusCode
import com.gym.server.service.MemberService
import com.gym.server.dto.request.UpdateProfileImageRequest
import com.gym.server.dto.toResponse
import com.gym.shared.domain.result.Result

fun Route.memberRoutes(memberService: MemberService) {
    route("/members") {
        get("/me") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()
            
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }

            when (val result = memberService.getMemberDetails(userId)) {
                is Result.Success -> {
                    val member = result.data
                    if (member == null) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respond(member.toResponse())
                    }
                }
                is Result.Error -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to result.message))
            }
        }
        
        put("/me/profile-image") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()
            
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }

            val request = call.receive<UpdateProfileImageRequest>()
            val result = memberService.updateProfileImage(userId, request.profileImageUrl)
            call.respondResultWithStatus(result, HttpStatusCode.OK)
        }
    }
}
