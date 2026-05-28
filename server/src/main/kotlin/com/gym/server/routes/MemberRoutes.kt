package com.gym.server.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import com.gym.server.database.Users

fun Route.memberRoutes() {
    route("/members") {
        get("/me") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()
            
            if (userId == null) {
                call.respond(io.ktor.http.HttpStatusCode.Unauthorized)
                return@get
            }

            val member = org.jetbrains.exposed.sql.transactions.transaction {
                val users = com.gym.server.database.Users
                val profiles = com.gym.server.database.MemberProfiles
                val payments = com.gym.server.database.Payments
                val plans = com.gym.server.database.Plans

                val row = (users innerJoin profiles)
                    .selectAll().where { users.id eq userId }
                    .singleOrNull() ?: return@transaction null

                val planRow = row[profiles.currentPlanId]?.let { planId ->
                    plans.selectAll().where { plans.id eq planId }.singleOrNull()
                }
                val planName = planRow?.get(plans.name)
                val weeklyLimit = planRow?.get(plans.weeklyLimit)

                // Weekly attendance calculation
                val now = kotlinx.datetime.Clock.System.now()
                val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
                val nowLocal = now.toLocalDateTime(tz)
                val today = nowLocal.date
                val daysFromMonday = today.dayOfWeek.ordinal
                val mondayDate = today.minus(daysFromMonday, kotlinx.datetime.DateTimeUnit.DAY)
                val mondayStart = kotlinx.datetime.LocalDateTime(mondayDate.year, mondayDate.monthNumber, mondayDate.dayOfMonth, 0, 0)

                val weeklyCount = com.gym.server.database.CheckIns.selectAll().where {
                    (com.gym.server.database.CheckIns.userId eq userId) and (com.gym.server.database.CheckIns.timestamp greaterEq mondayStart)
                }.count().toInt()

                val latestPayment = payments.selectAll().where { payments.userId eq userId }
                    .orderBy(payments.expirationDate, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(1)
                    .singleOrNull()
                
                val expDate = latestPayment?.get(payments.expirationDate)?.toInstant(tz)
                val isExpired = expDate == null || expDate < now

                com.gym.shared.domain.Member(
                    id = userId,
                    name = row[users.name],
                    email = row[users.email],
                    role = row[users.role],
                    joinDate = row[profiles.joinDate].toInstant(tz),
                    status = if (isExpired) "Expired" else "Active",
                    phone = row[profiles.phone],
                    currentPlan = planName,
                    expirationDate = expDate,
                    profileImageUrl = row[users.profileImageUrl],
                    weeklyAttendanceCount = weeklyCount,
                    weeklyAttendanceLimit = weeklyLimit
                )
            }

            if (member == null) {
                call.respond(io.ktor.http.HttpStatusCode.NotFound)
            } else {
                call.respond(member)
            }
        }
        put("/me/profile-image") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString()
            
            if (userId == null) {
                call.respond(io.ktor.http.HttpStatusCode.Unauthorized)
                return@put
            }

            val request = call.receive<Map<String, String>>()
            val imageUrl = request["profileImageUrl"] ?: return@put call.respond(io.ktor.http.HttpStatusCode.BadRequest)

            org.jetbrains.exposed.sql.transactions.transaction {
                Users.update({ Users.id eq userId }) {
                    it[Users.profileImageUrl] = imageUrl
                }
            }
            call.respond(io.ktor.http.HttpStatusCode.OK)
        }
    }
}
