package com.gym.server.routes

import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*

fun Route.adminRoutes() {
    get("/dashboard/summary") {
        val summary = org.jetbrains.exposed.sql.transactions.transaction {
            val users = com.gym.server.database.Users
            val profiles = com.gym.server.database.MemberProfiles
            val payments = com.gym.server.database.Payments
            val checkIns = com.gym.server.database.CheckIns
            val plans = com.gym.server.database.Plans
            
            val now = kotlinx.datetime.Clock.System.now()
            val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
            val nowLocal = now.toLocalDateTime(tz)
            val sevenDaysFromNowLocal = (now + kotlin.time.Duration.parse("7d")).toLocalDateTime(tz)
            
            val todayStartLocal = kotlinx.datetime.LocalDateTime(nowLocal.year, nowLocal.month, nowLocal.dayOfMonth, 0, 0)
            
            // 1. Get all members
            val memberIds = profiles.selectAll().map { it[profiles.userId] }
            
            var activeCount = 0
            var expiredCount = 0
            var expiringSoonCount = 0
            
            val overdueRiskList = mutableListOf<com.gym.shared.domain.RiskMember>()

            memberIds.forEach { mId ->
                val latestPayment = payments.selectAll().where { payments.userId eq mId }
                    .orderBy(payments.expirationDate, org.jetbrains.exposed.sql.SortOrder.DESC)
                    .limit(1)
                    .singleOrNull()
                
                val latestExp = latestPayment?.get(payments.expirationDate)
                
                if (latestExp == null || latestExp < nowLocal) {
                    expiredCount++
                } else {
                    activeCount++
                    if (latestExp < sevenDaysFromNowLocal) {
                        expiringSoonCount++
                        
                        val diff = latestExp.toInstant(tz).minus(now).inWholeDays.toInt()
                        val name = users.selectAll().where { users.id eq mId }.singleOrNull()?.get(users.name) ?: "Unknown"
                        overdueRiskList.add(com.gym.shared.domain.RiskMember(name, diff, latestPayment[payments.amount]))
                    }
                }
            }
            
            val todayCheckIns = checkIns.selectAll().where { checkIns.timestamp greaterEq todayStartLocal }.count().toInt()

            // Recent Arrivals
            val arrivals = (checkIns innerJoin users innerJoin plans)
                .selectAll().orderBy(checkIns.timestamp, org.jetbrains.exposed.sql.SortOrder.DESC).limit(5)
                .map { 
                    com.gym.shared.domain.Arrival(
                        name = it[users.name],
                        plan = it[plans.name],
                        timestamp = it[checkIns.timestamp].toInstant(tz)
                    )
                }

            // Revenue Flow (Last 7 months)
            val revenueFlowList = (0..6).reversed().map { monthsAgo ->
                val date = nowLocal.date.minus(monthsAgo, DateTimeUnit.MONTH)
                val monthStart = LocalDateTime(date.year, date.monthNumber, 1, 0, 0)
                val nextMonthDate = date.plus(1, DateTimeUnit.MONTH)
                val nextMonth = LocalDateTime(nextMonthDate.year, nextMonthDate.monthNumber, 1, 0, 0)
                
                val total = payments.selectAll().where { 
                    (payments.paymentDate greaterEq monthStart) and (payments.paymentDate less nextMonth)
                }.map { it[payments.amount] }.sum()
                
                com.gym.shared.domain.MonthlyRevenue(date.month.name.take(3), total)
            }
            
            com.gym.shared.domain.DashboardSummary(
                totalActiveMembers = activeCount,
                totalExpiredMembers = expiredCount,
                expiringSoonCount = expiringSoonCount,
                todayCheckInsCount = todayCheckIns,
                overdueRisk = overdueRiskList.sortedBy { it.daysRemaining }.take(5),
                recentArrivals = arrivals,
                revenueFlow = revenueFlowList
            )
        }
        call.respond(summary)
    }

    route("/admin") {
        get("/members") {
            val members = org.jetbrains.exposed.sql.transactions.transaction {
                (com.gym.server.database.Users innerJoin com.gym.server.database.MemberProfiles)
                    .selectAll()
                    .map { row ->
                        val profile = com.gym.server.database.MemberProfiles
                        val plans = com.gym.server.database.Plans
                        val payments = com.gym.server.database.Payments
                        val userId = row[com.gym.server.database.Users.id]
                        
                        val planRow = row[profile.currentPlanId]?.let { planId ->
                            plans.selectAll().where { plans.id eq planId }.singleOrNull()
                        }
                        val planName = planRow?.get(plans.name)
                        val weeklyLimit = planRow?.get(plans.weeklyLimit)

                        // Weekly attendance calculation
                        val now = kotlinx.datetime.Clock.System.now()
                        val tz = TimeZone.currentSystemDefault()
                        val nowLocal = now.toLocalDateTime(tz)
                        val today = nowLocal.date
                        val daysFromMonday = today.dayOfWeek.ordinal
                        val mondayDate = today.minus(daysFromMonday, kotlinx.datetime.DateTimeUnit.DAY)
                        val mondayStart = kotlinx.datetime.LocalDateTime(mondayDate.year, mondayDate.monthNumber, mondayDate.dayOfMonth, 0, 0)

                        val weeklyCount = com.gym.server.database.CheckIns.selectAll().where {
                            (com.gym.server.database.CheckIns.userId eq userId) and (com.gym.server.database.CheckIns.timestamp greaterEq mondayStart)
                        }.count().toInt()

                        // Find the latest expiration date for this user
                        val latestPayment = payments.selectAll().where { payments.userId eq userId }
                            .orderBy(payments.expirationDate, SortOrder.DESC)
                            .limit(1)
                            .singleOrNull()
                        
                        val expirationDateInstant = latestPayment?.get(payments.expirationDate)?.toInstant(tz)
                        val isExpired = expirationDateInstant == null || expirationDateInstant < now

                        com.gym.shared.domain.Member(
                            id = userId,
                            name = row[com.gym.server.database.Users.name],
                            email = row[com.gym.server.database.Users.email],
                            role = row[com.gym.server.database.Users.role],
                            joinDate = row[com.gym.server.database.MemberProfiles.joinDate].toInstant(tz),
                            status = if (isExpired) "Expired" else "Active",
                            phone = row[com.gym.server.database.MemberProfiles.phone],
                            dni = row[com.gym.server.database.MemberProfiles.dni],
                            currentPlan = planName,
                            expirationDate = expirationDateInstant,
                            profileImageUrl = row[com.gym.server.database.Users.profileImageUrl],
                            weeklyAttendanceCount = weeklyCount,
                            weeklyAttendanceLimit = weeklyLimit
                        )
                    }
            }
            call.respond(members)
        }

        get("/plans") {
            val plans = org.jetbrains.exposed.sql.transactions.transaction {
                com.gym.server.database.Plans.selectAll().map {
                    com.gym.shared.domain.GymPlan(
                        id = it[com.gym.server.database.Plans.id],
                        name = it[com.gym.server.database.Plans.name],
                        price = it[com.gym.server.database.Plans.price],
                        durationDays = it[com.gym.server.database.Plans.durationDays],
                        description = it[com.gym.server.database.Plans.description]
                    )
                }
            }
            call.respond(plans)
        }

        post("/plans") {
            val request = call.receive<com.gym.shared.domain.GymPlan>()
            org.jetbrains.exposed.sql.transactions.transaction {
                com.gym.server.database.Plans.insert {
                    it[id] = request.id.ifEmpty { "p-${java.util.UUID.randomUUID().toString().take(8)}" }
                    it[name] = request.name
                    it[price] = request.price
                    it[durationDays] = request.durationDays
                    it[description] = request.description
                }
            }
            call.respond(io.ktor.http.HttpStatusCode.Created)
        }

        put("/plans/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val request = call.receive<com.gym.shared.domain.GymPlan>()
            org.jetbrains.exposed.sql.transactions.transaction {
                com.gym.server.database.Plans.update({ com.gym.server.database.Plans.id eq id }) {
                    it[name] = request.name
                    it[price] = request.price
                    it[durationDays] = request.durationDays
                    it[description] = request.description
                }
            }
            call.respond(io.ktor.http.HttpStatusCode.OK)
        }

        post("/members") {
            val request = call.receive<com.gym.shared.domain.MemberRequest>()
            val newId = "u-${java.util.UUID.randomUUID()}"
            
            try {
                org.jetbrains.exposed.sql.transactions.transaction {
                    com.gym.server.database.Users.insert {
                        it[id] = newId
                        it[email] = request.email
                        it[passwordHash] = "default_pass"
                        it[name] = request.name
                        it[role] = com.gym.shared.domain.UserRole.MEMBER
                        it[createdAt] = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    }
                    com.gym.server.database.MemberProfiles.insert {
                        it[userId] = newId
                        it[dni] = request.dni
                        it[phone] = request.phone
                        it[joinDate] = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        it[isActive] = request.isActive
                        it[currentPlanId] = if (request.planId.isNullOrEmpty()) null else request.planId
                    }

                    // Mandatory initial payment
                    val now = kotlinx.datetime.Clock.System.now()
                    val tz = TimeZone.currentSystemDefault()
                    val expiration = now.plus(30, kotlinx.datetime.DateTimeUnit.DAY, tz).toLocalDateTime(tz)

                    com.gym.server.database.Payments.insert {
                        it[id] = java.util.UUID.randomUUID().toString()
                        it[userId] = newId
                        it[amount] = request.paymentAmount
                        it[paymentDate] = now.toLocalDateTime(tz)
                        it[expirationDate] = expiration
                        it[method] = request.paymentMethod
                        it[com.gym.server.database.Payments.timestamp] = now.toLocalDateTime(tz)
                    }
                }
                call.respond(io.ktor.http.HttpStatusCode.Created)
            } catch (e: Exception) {
                // Log for debugging
                println("Error creating member: ${e.message}")
                e.printStackTrace()
                
                // Get full error message including causes
                var currentError: Throwable? = e
                var errorMessageText = ""
                while (currentError != null) {
                    errorMessageText += " " + currentError.toString()
                    currentError = currentError.cause
                }
                
                val isDuplicateEmail = errorMessageText.contains("users_email_unique", ignoreCase = true)
                val isDuplicateDni = errorMessageText.contains("member_profiles_dni_unique", ignoreCase = true)
                
                when {
                    isDuplicateEmail -> {
                        call.respond(io.ktor.http.HttpStatusCode.Conflict, mapOf("error" to "This Email is already registered in our system"))
                    }
                    isDuplicateDni -> {
                        call.respond(io.ktor.http.HttpStatusCode.Conflict, mapOf("error" to "This DNI is already registered in our system"))
                    }
                    else -> {
                        call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Database error")))
                    }
                }
            }
        }

        put("/members/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val request = call.receive<com.gym.shared.domain.MemberRequest>()
            org.jetbrains.exposed.sql.transactions.transaction {
                com.gym.server.database.Users.update({ com.gym.server.database.Users.id eq id }) {
                    it[email] = request.email
                    it[name] = request.name
                }
                com.gym.server.database.MemberProfiles.update({ com.gym.server.database.MemberProfiles.userId eq id }) {
                    it[dni] = request.dni
                    it[phone] = request.phone
                    it[currentPlanId] = if (request.planId.isNullOrEmpty()) null else request.planId
                }
            }
            call.respond(io.ktor.http.HttpStatusCode.OK)
        }
    }
}
