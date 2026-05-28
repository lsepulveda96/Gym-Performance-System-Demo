package com.gym.server.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import com.gym.shared.domain.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import com.gym.server.database.Users
import com.gym.server.database.MemberProfiles

fun Route.authRoutes() {
    route("/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            
            val authResult = transaction {
                val user = Users.selectAll().where { Users.email eq request.email }.singleOrNull()
                
                if (user == null) return@transaction null
                
                val isValid = when (user[Users.role]) {
                    UserRole.MEMBER -> {
                        val profile = MemberProfiles.selectAll().where { MemberProfiles.userId eq user[Users.id] }.singleOrNull()
                        profile != null && profile[MemberProfiles.dni] == request.password // DNI is passed in password field for members
                    }
                    UserRole.OWNER, UserRole.RECEPTION -> {
                        user[Users.passwordHash] == request.password
                    }
                    else -> false
                }
                
                if (isValid) {
                    GymUser(
                        id = user[Users.id],
                        email = user[Users.email],
                        name = user[Users.name],
                        role = user[Users.role],
                        profileImageUrl = user[Users.profileImageUrl]
                    )
                } else null
            }

            if (authResult != null) {
                val token = JWT.create()
                    .withAudience("gym-system")
                    .withIssuer("https://gym-system.com")
                    .withClaim("userId", authResult.id)
                    .withClaim("role", authResult.role.name)
                    .withExpiresAt(Date(System.currentTimeMillis() + 2592000000L)) // 30 days
                    .sign(Algorithm.HMAC256("secret"))

                call.respond(AuthResponse(
                    token = token,
                    user = authResult
                ))
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }

        
        post("/register") {
            // Placeholder logic
            call.respond(HttpStatusCode.Created)
        }
    }
}
