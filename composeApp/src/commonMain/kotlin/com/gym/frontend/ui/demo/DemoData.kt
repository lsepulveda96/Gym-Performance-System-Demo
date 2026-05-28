package com.gym.frontend.ui.demo

import com.gym.shared.domain.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * All mock data used when DEMO_MODE=true.
 * No backend calls are made; this data drives every screen.
 */
object DemoData {

    val plans = listOf(
        GymPlan("p-3pw",       "3 per week",          30000.0, 30, "Access 3 times per week", 3),
        GymPlan("p-unlimited", "Unlimited",            34000.0, 30, "Unlimited access",         null),
        GymPlan("p-elite",     "Unlimited + Routine",  37000.0, 30, "Unlimited + personalized routine", null)
    )

    /* ─── Auth ─────────────────────────────────────────────────────────── */

    val adminUser = GymUser(
        id = "demo-admin",
        email = "admin@demo.com",
        name = "Alex Rivera",
        role = UserRole.OWNER
    )

    val memberUser = GymUser(
        id = "demo-member",
        email = "member@demo.com",
        name = "Jordan Silva",
        role = UserRole.MEMBER
    )

    val adminAuthResponse = AuthResponse(
        token = "demo-admin-token",
        user = adminUser
    )

    val memberAuthResponse = AuthResponse(
        token = "demo-member-token",
        user = memberUser
    )

    /* ─── Members list ──────────────────────────────────────────────────── */

    private val now: Instant get() = Clock.System.now()

    val members: List<Member> = listOf(
        Member(
            id = "m1", name = "Jordan Silva",   email = "jordan@demo.com",
            role = UserRole.MEMBER, joinDate = now - 120.days,
            status = "Active", currentPlan = "Unlimited",
            phone = "+1 555-0101", dni = "11111111",
            expirationDate = now + 12.days,
            weeklyAttendanceCount = 2, weeklyAttendanceLimit = null
        ),
        Member(
            id = "m2", name = "Sam Carter",     email = "sam@demo.com",
            role = UserRole.MEMBER, joinDate = now - 80.days,
            status = "Active", currentPlan = "3 per week",
            phone = "+1 555-0102", dni = "22222222",
            expirationDate = now + 25.days,
            weeklyAttendanceCount = 1, weeklyAttendanceLimit = 3
        ),
        Member(
            id = "m3", name = "Taylor Brooks",  email = "taylor@demo.com",
            role = UserRole.MEMBER, joinDate = now - 200.days,
            status = "Active", currentPlan = "Unlimited + Routine",
            phone = "+1 555-0103", dni = "33333333",
            expirationDate = now + 3.days,
            weeklyAttendanceCount = 4, weeklyAttendanceLimit = null
        ),
        Member(
            id = "m4", name = "Morgan Lee",     email = "morgan@demo.com",
            role = UserRole.MEMBER, joinDate = now - 45.days,
            status = "Inactive", currentPlan = "3 per week",
            phone = "+1 555-0104", dni = "44444444",
            expirationDate = now - 5.days,
            weeklyAttendanceCount = 0, weeklyAttendanceLimit = 3
        ),
        Member(
            id = "m5", name = "Casey Kim",      email = "casey@demo.com",
            role = UserRole.MEMBER, joinDate = now - 300.days,
            status = "Active", currentPlan = "Unlimited",
            phone = "+1 555-0105", dni = "55555555",
            expirationDate = now + 18.days,
            weeklyAttendanceCount = 3, weeklyAttendanceLimit = null
        ),
        Member(
            id = "m6", name = "Riley Davis",    email = "riley@demo.com",
            role = UserRole.MEMBER, joinDate = now - 15.days,
            status = "Active", currentPlan = "Unlimited + Routine",
            phone = "+1 555-0106", dni = "66666666",
            expirationDate = now + 15.days,
            weeklyAttendanceCount = 2, weeklyAttendanceLimit = null
        )
    )

    /* ─── Member "me" profile ───────────────────────────────────────────── */

    val memberMe = members.first()   // Jordan Silva

    /* ─── Attendance / CheckIns ─────────────────────────────────────────── */

    val checkIns: List<CheckIn> = listOf(
        CheckIn("ci1", "m1", now - 1.days,  "p-unlimited"),
        CheckIn("ci2", "m1", now - 3.days,  "p-unlimited"),
        CheckIn("ci3", "m1", now - 7.days,  "p-unlimited"),
        CheckIn("ci4", "m1", now - 10.days, "p-unlimited"),
        CheckIn("ci5", "m1", now - 14.days, "p-unlimited")
    )

    /* ─── Payments ──────────────────────────────────────────────────────── */

    val payments: List<Payment> = listOf(
        Payment("pay1", "m1", 34000.0, now - 30.days, now + 12.days, "Cash"),
        Payment("pay2", "m1", 34000.0, now - 60.days, now - 30.days, "Transfer"),
        Payment("pay3", "m1", 34000.0, now - 90.days, now - 60.days, "Cash")
    )

    /* ─── Dashboard ─────────────────────────────────────────────────────── */

    val dashboardSummary = DashboardSummary(
        totalActiveMembers   = 5,
        totalExpiredMembers  = 1,
        expiringSoonCount    = 2,
        todayCheckInsCount   = 3,
        overdueRisk = listOf(
            RiskMember("Morgan Lee",    -5,   30000.0),
            RiskMember("Taylor Brooks",  3,   37000.0)
        ),
        recentArrivals = listOf(
            Arrival("Jordan Silva",  "Unlimited",          now - 10.minutes),
            Arrival("Casey Kim",     "Unlimited",          now - 35.minutes),
            Arrival("Riley Davis",   "Unlimited + Routine", now - 1.hours),
            Arrival("Sam Carter",    "3 per week",          now - 2.hours)
        ),
        revenueFlow = listOf(
            MonthlyRevenue("Jul", 210000.0),
            MonthlyRevenue("Aug", 187000.0),
            MonthlyRevenue("Sep", 224000.0),
            MonthlyRevenue("Oct", 198000.0),
            MonthlyRevenue("Nov", 245000.0),
            MonthlyRevenue("Dec", 262000.0)
        )
    )

    /* ─── QR / Access ───────────────────────────────────────────────────── */

    fun demoQrToken(): QRToken {
        val ts = Clock.System.now()
        return QRToken(
            token     = "gym:access:demo-member:${ts.epochSeconds}:DEMO0000DEMO0000",
            expiresAt = ts + 5.minutes
        )
    }

    val demoValidationGranted = AccessValidationResponse(
        success                  = true,
        message                  = "Access granted",
        memberName               = "Jordan Silva",
        planName                 = "Unlimited",
        weeklyAccessLimit        = null,
        currentWeeklyAccessCount = 3,
        expirationDate           = Clock.System.now() + 12.days
    )

    val demoValidationExpired = AccessValidationResponse(
        success    = false,
        message    = "Membership expired",
        memberName = "Morgan Lee",
        planName   = "3 per week",
        weeklyAccessLimit        = 3,
        currentWeeklyAccessCount = 0,
        expirationDate           = Clock.System.now() - 5.days
    )
}
