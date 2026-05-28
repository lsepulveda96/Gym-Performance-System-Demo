package com.gym.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import com.gym.server.routes.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Gym Management API is Running!")
        }
        
        authRoutes()
        adminRoutes()
        paymentRoutes()
        
        authenticate("auth-jwt") {
            memberRoutes()
            checkInRoutes()
            accessRoutes()
        }
    }
}
