package com.gym.server.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.request.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.gym.shared.domain.QRToken
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds
import java.util.Date

fun Route.checkInRoutes() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "secret"
    val algorithm = Algorithm.HMAC256(jwtSecret)

    route("/checkin") {
        get("/generate") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString() ?: "unknown"
            
            val expiresAt = Clock.System.now() + 20.seconds
            val token = JWT.create()
                .withClaim("userId", userId)
                .withClaim("purpose", "checkin")
                .withExpiresAt(Date.from(java.time.Instant.ofEpochSecond(expiresAt.epochSeconds)))
                .sign(algorithm)
            
            call.respond(QRToken(token, expiresAt))
        }
        
        post("/verify") {
            // Reception scans QR and sends the token here
            val token = call.receiveText()
            try {
                val verifier = JWT.require(algorithm).withClaim("purpose", "checkin").build()
                val decoded = verifier.verify(token)
                val userId = decoded.getClaim("userId").asString()
                
                // Logic to check subscription and log check-in would go here
                call.respond(mapOf("status" to "success", "userId" to userId))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Forbidden, "Invalid or expired token")
            }
        }
    }
}
