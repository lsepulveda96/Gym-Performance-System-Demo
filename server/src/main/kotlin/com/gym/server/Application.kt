package com.gym.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.gym.server.plugins.*
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Database initialization
    val dbUrl = System.getenv("JDBC_DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/gym_db"
    val dbUser = System.getenv("JDBC_DATABASE_USER") ?: "postgres"
    val dbPassword = System.getenv("JDBC_DATABASE_PASSWORD") ?: "postgres"
    
    try {
        Database.connect(dbUrl, driver = "org.postgresql.Driver", user = dbUser, password = dbPassword)
        // Check connection by running a simple transaction
        org.jetbrains.exposed.sql.transactions.transaction {
            exec("SELECT 1")
        }
        println("Connected to PostgreSQL at $dbUrl")
    } catch (e: Exception) {
        println("Warning: PostgreSQL connection failed (${e.message}). Falling back to H2 In-Memory.")
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    }
    
    val demoMode = (System.getenv("DEMO_MODE") ?: "false").trim().lowercase() in setOf("true", "1", "yes")

    // Create tables
    org.jetbrains.exposed.sql.transactions.transaction {
        val tables = arrayOf(
            com.gym.server.database.Users,
            com.gym.server.database.Plans,
            com.gym.server.database.MemberProfiles,
            com.gym.server.database.Subscriptions,
            com.gym.server.database.CheckIns,
            com.gym.server.database.Payments
        )
        
        org.jetbrains.exposed.sql.SchemaUtils.create(*tables)
        org.jetbrains.exposed.sql.SchemaUtils.addMissingColumnsStatements(*tables).forEach { exec(it) }

        // Update specific test member
        com.gym.server.database.MemberProfiles.update({ com.gym.server.database.MemberProfiles.userId eq "u-member-1" }) {
            it[dni] = "12345678"
        }
        
        // Assign unique dummy DNIs to others who are still null to avoid unique constraint violation
        com.gym.server.database.MemberProfiles.selectAll().where { com.gym.server.database.MemberProfiles.dni.isNull() }.forEach { row ->
            val uid = row[com.gym.server.database.MemberProfiles.userId]
            com.gym.server.database.MemberProfiles.update({ com.gym.server.database.MemberProfiles.userId eq uid }) {
                it[dni] = "TEMP-$uid"
            }
        }

        // Ensure test admin user exists
        val testEmail = "test@hotmail.com"
        val testExists = com.gym.server.database.Users.selectAll().where { com.gym.server.database.Users.email eq testEmail }.count() > 0
        if (!testExists) {
            com.gym.server.database.Users.insert {
                it[id] = "u-test-reception"
                it[email] = testEmail
                it[passwordHash] = "123"
                it[name] = "Test Receptionist"
                it[role] = com.gym.shared.domain.UserRole.RECEPTION
                it[createdAt] = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            }
        }

        if (demoMode) {
            // Demo credentials for public deployments
            val demoAdminEmail = "admin@demo.com"
            val demoMemberEmail = "member@demo.com"

            val adminExists = com.gym.server.database.Users
                .selectAll().where { com.gym.server.database.Users.email eq demoAdminEmail }
                .count() > 0
            if (!adminExists) {
                com.gym.server.database.Users.insert {
                    it[id] = "u-demo-admin"
                    it[email] = demoAdminEmail
                    it[passwordHash] = "1234"
                    it[name] = "Demo Admin"
                    it[role] = com.gym.shared.domain.UserRole.OWNER
                    it[createdAt] = kotlinx.datetime.Clock.System.now()
                        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                }
            }

            val memberExists = com.gym.server.database.Users
                .selectAll().where { com.gym.server.database.Users.email eq demoMemberEmail }
                .count() > 0
            if (!memberExists) {
                val now = kotlinx.datetime.Clock.System.now()
                    .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())

                com.gym.server.database.Users.insert {
                    it[id] = "u-demo-member"
                    it[email] = demoMemberEmail
                    it[passwordHash] = "unused"
                    it[name] = "Demo Member"
                    it[role] = com.gym.shared.domain.UserRole.MEMBER
                    it[createdAt] = now
                }
                com.gym.server.database.MemberProfiles.insert {
                    it[userId] = "u-demo-member"
                    it[dni] = "1234"
                    it[phone] = null
                    it[joinDate] = now
                    it[isActive] = true
                    it[currentPlanId] = "p-unlimited"
                }
            }
        }

        // --- Robust Plans Seeding & Sync ---
        val defaultPlans = listOf(
            com.gym.shared.domain.GymPlan("p-3pw", "3 per week", 30000.0, 30, "Access 3 times per week", 3),
            com.gym.shared.domain.GymPlan("p-unlimited", "Unlimited", 34000.0, 30, "Unlimited access", null),
            com.gym.shared.domain.GymPlan("p-elite", "Unlimited + Routine", 37000.0, 30, "Unlimited access + personalized routine", null)
        )

        defaultPlans.forEach { plan ->
            val exists = com.gym.server.database.Plans.selectAll().where { com.gym.server.database.Plans.id eq plan.id }.count() > 0
            if (!exists) {
                com.gym.server.database.Plans.insert {
                    it[id] = plan.id
                    it[name] = plan.name
                    it[price] = plan.price
                    it[durationDays] = plan.durationDays
                    it[description] = plan.description
                    it[weeklyLimit] = plan.weeklyLimit
                }
            } else {
                com.gym.server.database.Plans.update({ com.gym.server.database.Plans.id eq plan.id }) {
                    it[name] = plan.name
                    it[price] = plan.price
                    it[durationDays] = plan.durationDays
                    it[description] = plan.description
                    it[weeklyLimit] = plan.weeklyLimit
                }
            }
        }

        // Simple seed for initial users if empty
        if (com.gym.server.database.Users.selectAll().count() == 0L) {

            val adminId = "u-admin"
            com.gym.server.database.Users.insert {
                it[id] = adminId
                it[email] = "admin@kinetic.io"
                it[passwordHash] = "hash"
                it[name] = "Admin User"
                it[role] = com.gym.shared.domain.UserRole.OWNER
                it[createdAt] = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            }
            
            // Seed a member
            val memberId = "u-member-1"
            com.gym.server.database.Users.insert {
                it[id] = memberId
                it[email] = "member1@test.com"
                it[passwordHash] = "hash"
                it[name] = "Alex Rivera"
                it[role] = com.gym.shared.domain.UserRole.MEMBER
                it[createdAt] = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            }
            com.gym.server.database.MemberProfiles.insert {
                it[userId] = memberId
                it[dni] = "12345678"
                it[phone] = "+123456789"
                it[joinDate] = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                it[isActive] = true
                it[currentPlanId] = "p-unlimited"
            }
        }
    }
    
    configureSerialization()
    configureHTTP()
    configureSecurity()
    configureRouting()
}
