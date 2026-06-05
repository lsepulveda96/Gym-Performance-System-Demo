package com.gym.server.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.HttpStatusCode
import com.gym.server.service.DashboardService
import com.gym.server.service.MemberService
import com.gym.server.service.PlanService
import com.gym.server.dto.request.CreateMemberRequest
import com.gym.server.dto.request.UpdateMemberRequest
import com.gym.server.dto.request.CreatePlanRequest
import com.gym.server.dto.request.UpdatePlanRequest
import com.gym.server.dto.toDomain
import com.gym.server.dto.toResponse
import com.gym.shared.domain.result.Result

fun Route.adminRoutes(
    dashboardService: DashboardService,
    memberService: MemberService,
    planService: PlanService
) {
    get("/dashboard/summary") {
        val result = dashboardService.getSummary()
        call.respondResult(result) { it.toResponse() }
    }

    route("/admin") {
        get("/members") {
            val result = memberService.getAllMembersDetails()
            call.respondResult(result) { members -> members.map { it.toResponse() } }
        }

        get("/plans") {
            val result = planService.getAllPlans()
            call.respondResult(result) { plans -> plans.map { it.toResponse() } }
        }

        post("/plans") {
            val request = call.receive<CreatePlanRequest>()
            val result = planService.createPlan(request.toDomain())
            call.respondResultWithStatus(result, HttpStatusCode.Created)
        }

        put("/plans/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<UpdatePlanRequest>()
            val result = planService.updatePlan(id, request.toDomain(id))
            call.respondResultWithStatus(result, HttpStatusCode.OK)
        }

        post("/members") {
            val request = call.receive<CreateMemberRequest>()
            val result = memberService.createMember(request.toDomain())
            
            when (result) {
                is Result.Success -> call.respond(HttpStatusCode.Created)
                is Result.Error -> {
                    val msg = result.message
                    if (msg.contains("Email is already registered") || msg.contains("DNI is already registered")) {
                        call.respond(HttpStatusCode.Conflict, mapOf("error" to msg))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to msg))
                    }
                }
            }
        }

        put("/members/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<UpdateMemberRequest>()
            val result = memberService.updateMember(id, request.toDomain())
            call.respondResultWithStatus(result, HttpStatusCode.OK)
        }
    }
}
