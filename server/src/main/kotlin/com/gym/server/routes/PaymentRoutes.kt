package com.gym.server.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import com.gym.shared.domain.*
import com.gym.server.database.Payments
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.paymentRoutes() {
    route("/payments") {
        post {
            val request = call.receive<PaymentRequest>()
            val now = Clock.System.now()
            val paymentDateLocal = now.toLocalDateTime(TimeZone.currentSystemDefault())
            // Calculate expiration (e.g. +30 days)
            val expirationDateLocal = now.plus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.currentSystemDefault())

            val newId = UUID.randomUUID().toString()
            
            transaction {
                Payments.insert {
                    it[id] = newId
                    it[userId] = request.userId
                    it[amount] = request.amount
                    it[paymentDate] = paymentDateLocal
                    it[expirationDate] = expirationDateLocal
                    it[method] = request.method
                    it[timestamp] = paymentDateLocal
                }
            }

            call.respond(HttpStatusCode.Created, mapOf("id" to newId))
        }

        get("/member/{memberId}") {
            val memberId = call.parameters["memberId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            
            val history = transaction {
                Payments.selectAll().where { Payments.userId eq memberId }
                    .orderBy(Payments.paymentDate, SortOrder.DESC)
                    .map {
                        Payment(
                            id = it[Payments.id],
                            userId = it[Payments.userId],
                            amount = it[Payments.amount],
                            paymentDate = it[Payments.paymentDate].toInstant(TimeZone.currentSystemDefault()),
                            expirationDate = it[Payments.expirationDate].toInstant(TimeZone.currentSystemDefault()),
                            method = it[Payments.method]
                        )
                    }
            }
            
            call.respond(history)
        }
    }
}
