package com.gym.server.database

import com.gym.shared.domain.SubscriptionStatus
import com.gym.shared.domain.UserRole
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Users : Table("users") {
    val id = varchar("id", 50)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 255)
    val role = enumerationByName("role", 20, UserRole::class)
    val createdAt = datetime("created_at")
    val profileImageUrl = varchar("profile_image_url", 500).nullable()
    
    override val primaryKey = PrimaryKey(id)
}

object MemberProfiles : Table("member_profiles") {
    val userId = reference("user_id", Users.id)
    val dni = varchar("dni", 50).uniqueIndex().nullable()
    val phone = varchar("phone", 50).nullable()
    val joinDate = datetime("join_date")
    val isActive = bool("is_active").default(true)
    val currentPlanId = reference("current_plan_id", Plans.id).nullable()
    
    override val primaryKey = PrimaryKey(userId)
}

object Plans : Table("plans") {
    val id = varchar("id", 50)
    val name = varchar("name", 100)
    val price = double("price")
    val durationDays = integer("duration_days")
    val description = text("description").nullable()
    val weeklyLimit = integer("weekly_limit").nullable()
    
    override val primaryKey = PrimaryKey(id)
}

object Subscriptions : Table("subscriptions") {
    val id = varchar("id", 36)
    val userId = reference("user_id", Users.id)
    val planId = reference("plan_id", Plans.id)
    val startDate = datetime("start_date")
    val endDate = datetime("end_date")
    val status = enumerationByName("status", 20, SubscriptionStatus::class)
    
    override val primaryKey = PrimaryKey(id)
}

object CheckIns : Table("check_ins") {
    val id = varchar("id", 36)
    val userId = reference("user_id", Users.id)
    val timestamp = datetime("timestamp")
    val planIdAtTime = reference("plan_id", Plans.id)
    
    override val primaryKey = PrimaryKey(id)
}

object Payments : Table("payments") {
    val id = varchar("id", 36)
    val userId = reference("user_id", Users.id)
    val amount = double("amount")
    val paymentDate = datetime("payment_date")
    val expirationDate = datetime("expiration_date")
    val method = varchar("method", 50)
    val timestamp = datetime("timestamp").defaultExpression(org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime)
    
    override val primaryKey = PrimaryKey(id)
}
