package com.gym.server.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.http.*
import com.gym.shared.domain.QRToken
import com.gym.shared.domain.AccessValidationRequest
import com.gym.shared.domain.AccessValidationResponse
import com.gym.server.database.Users
import com.gym.server.database.MemberProfiles
import com.gym.server.database.CheckIns
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration.Companion.minutes
import java.util.UUID

private const val QR_SECRET = "kinetic-gym-super-secret-2026"

private object AccessMessages {
    const val GRANTED = "Access granted"
    const val EXPIRED = "Membership expired"
    const val WEEKLY_LIMIT = "Weekly limit reached"
    const val INVALID_QR = "Invalid QR"
    const val QR_EXPIRED = "QR expired"
    const val MEMBER_NOT_FOUND = "Member not found"
    const val TAMPERED = "Invalid QR"
}

private fun generateSignature(memberId: String, timestamp: Long): String {
    val input = memberId + timestamp + QR_SECRET
    val md = java.security.MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray())
    return digest.fold("") { str, it -> str + "%02x".format(it) }.take(16)
}

fun Route.accessRoutes() {
    route("/access") {
        post("/validate") {
            handleAccessValidation(call)
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
                val signature = generateSignature(userId, timestamp)
                val token = "gym:access:${userId}:${timestamp}:${signature}"
                val expiresAt = Clock.System.now().plus(5.minutes)

                call.respond(QRToken(token, expiresAt))
            }

            get("/member/{memberId}") {
                val memberId = call.parameters["memberId"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                val logs = transaction {
                    CheckIns.selectAll()
                        .where { CheckIns.userId eq memberId }
                        .orderBy(CheckIns.timestamp, SortOrder.DESC)
                        .map { row ->
                            com.gym.shared.domain.CheckIn(
                                id = row[CheckIns.id],
                                userId = row[CheckIns.userId],
                                timestamp = row[CheckIns.timestamp].toInstant(TimeZone.currentSystemDefault()),
                                planIdAtTime = row[CheckIns.planIdAtTime]
                            )
                        }
                }
                call.respond(logs)
            }
        }
    }
}

private suspend fun handleAccessValidation(call: ApplicationCall) {
    val request = call.receive<AccessValidationRequest>()
    val code = request.code.trim()

    val parts = code.split(":")
    if (parts.size != 5 || parts[0] != "gym" || parts[1] != "access") {
        call.respond(denied(AccessMessages.INVALID_QR))
        return
    }

    val memberId = parts[2]
    val timestampEpoch = parts[3].toLongOrNull() ?: 0L
    val signature = parts[4]
    val timestamp = Instant.fromEpochSeconds(timestampEpoch)

    val expectedSignature = generateSignature(memberId, timestampEpoch)
    if (signature != expectedSignature) {
        call.respond(denied(AccessMessages.TAMPERED))
        return
    }

    val now = Clock.System.now()
    val diff = now.epochSeconds - timestamp.epochSeconds
    if (diff > 300 || diff < -60) {
        call.respond(denied(AccessMessages.QR_EXPIRED))
        return
    }

    val result = transaction {
        val user = Users
            .innerJoin(MemberProfiles)
            .selectAll().where { Users.id eq memberId }
            .singleOrNull()

        if (user == null) {
            return@transaction denied(AccessMessages.MEMBER_NOT_FOUND)
        }

        val name = user[Users.name]
        val nowLocal = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val tz = kotlinx.datetime.TimeZone.currentSystemDefault()

        val latestPayment = com.gym.server.database.Payments
            .selectAll().where { com.gym.server.database.Payments.userId eq memberId }
            .orderBy(com.gym.server.database.Payments.expirationDate, SortOrder.DESC)
            .limit(1)
            .singleOrNull()

        val expirationDate = latestPayment?.get(com.gym.server.database.Payments.expirationDate)
        val isExpired = expirationDate == null || expirationDate < nowLocal

        val today = nowLocal.date
        val daysFromMonday = today.dayOfWeek.ordinal
        val mondayDate = today.minus(daysFromMonday, kotlinx.datetime.DateTimeUnit.DAY)
        val mondayStart = kotlinx.datetime.LocalDateTime(mondayDate.year, mondayDate.monthNumber, mondayDate.dayOfMonth, 0, 0)

        val weeklyCount = CheckIns.selectAll().where {
            (CheckIns.userId eq memberId) and (CheckIns.timestamp greaterEq mondayStart)
        }.count().toInt()

        val planId = user[MemberProfiles.currentPlanId]
        val plan = if (planId != null) {
            com.gym.server.database.Plans.selectAll().where { com.gym.server.database.Plans.id eq planId }.singleOrNull()
        } else null

        val planName = plan?.get(com.gym.server.database.Plans.name) ?: "No Plan"
        val weeklyLimit = plan?.get(com.gym.server.database.Plans.weeklyLimit)

        val respBase = AccessValidationResponse(
            success = false,
            message = "",
            memberName = name,
            weeklyAccessLimit = weeklyLimit,
            currentWeeklyAccessCount = weeklyCount,
            planName = planName,
            expirationDate = expirationDate?.toInstant(tz)
        )

        if (isExpired) {
            return@transaction respBase.copy(message = AccessMessages.EXPIRED)
        }

        if (weeklyLimit != null && weeklyCount >= weeklyLimit) {
            return@transaction respBase.copy(message = AccessMessages.WEEKLY_LIMIT)
        }

        CheckIns.insert {
            it[id] = UUID.randomUUID().toString()
            it[CheckIns.userId] = memberId
            it[CheckIns.timestamp] = nowLocal
            it[CheckIns.planIdAtTime] = user[MemberProfiles.currentPlanId] ?: "p-basic"
        }

        respBase.copy(
            success = true,
            message = AccessMessages.GRANTED,
            currentWeeklyAccessCount = weeklyCount + 1
        )
    }

    call.respond(result)
}

private fun denied(message: String, memberName: String? = null) =
    AccessValidationResponse(success = false, message = message, memberName = memberName)
