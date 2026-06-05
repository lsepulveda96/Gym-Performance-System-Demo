package com.gym.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import com.gym.server.routes.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val authService by inject<com.gym.server.service.AuthService>()
    val dashboardService by inject<com.gym.server.service.DashboardService>()
    val memberService by inject<com.gym.server.service.MemberService>()
    val planService by inject<com.gym.server.service.PlanService>()
    val paymentService by inject<com.gym.server.service.PaymentService>()
    val accessService by inject<com.gym.server.service.AccessService>()

    routing {
        get("/") {
            call.respondText("Gym Management API is Running!")
        }
        
        authRoutes(authService)
        adminRoutes(dashboardService, memberService, planService)
        paymentRoutes(paymentService)
        
        authenticate("auth-jwt") {
            memberRoutes(memberService)
            checkInRoutes()
            accessRoutes(accessService)
        }
    }
}
