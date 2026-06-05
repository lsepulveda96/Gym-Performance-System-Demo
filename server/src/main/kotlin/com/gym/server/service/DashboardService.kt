package com.gym.server.service

import com.gym.server.repository.*
import com.gym.shared.domain.*
import com.gym.shared.domain.result.Result
import kotlinx.datetime.*

class DashboardService(
    private val userRepository: UserRepository,
    private val paymentRepository: PaymentRepository,
    private val checkInRepository: CheckInRepository,
    private val planRepository: PlanRepository
) {
    suspend fun getSummary(): Result<DashboardSummary> {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val nowLocal = now.toLocalDateTime(tz)
        val sevenDaysFromNowLocal = now.plus(7, DateTimeUnit.DAY, tz).toLocalDateTime(tz)
        val todayStartLocal = LocalDateTime(nowLocal.year, nowLocal.month, nowLocal.dayOfMonth, 0, 0)
        
        val usersResult = userRepository.findAllUsersWithProfiles()
        if (usersResult is Result.Error) return Result.Error(usersResult.message, usersResult.cause)
        val usersWithProfiles = (usersResult as Result.Success).data

        val paymentsResult = paymentRepository.findLatestPaymentsForAllUsers()
        if (paymentsResult is Result.Error) return Result.Error(paymentsResult.message, paymentsResult.cause)
        val latestPayments = (paymentsResult as Result.Success).data
        
        var activeCount = 0
        var expiredCount = 0
        var expiringSoonCount = 0
        val overdueRiskList = mutableListOf<RiskMember>()
        
        for ((user, profile, _) in usersWithProfiles) {
            val latestExp = latestPayments[user.id]?.expirationDate?.toLocalDateTime(tz)
            
            if (latestExp == null || latestExp < nowLocal) {
                expiredCount++
            } else {
                activeCount++
                if (latestExp < sevenDaysFromNowLocal) {
                    expiringSoonCount++
                    val diff = latestExp.toInstant(tz).minus(now).inWholeDays.toInt()
                    overdueRiskList.add(RiskMember(user.name, diff, latestPayments[user.id]?.amount ?: 0.0))
                }
            }
        }
        
        val todayCheckInsResult = checkInRepository.countCheckInsSince(todayStartLocal.toInstant(tz))
        if (todayCheckInsResult is Result.Error) return Result.Error(todayCheckInsResult.message, todayCheckInsResult.cause)
        val todayCheckIns = (todayCheckInsResult as Result.Success).data
        
        val recentCheckInsResult = checkInRepository.getRecentCheckIns(5)
        if (recentCheckInsResult is Result.Error) return Result.Error(recentCheckInsResult.message, recentCheckInsResult.cause)
        val recentRawCheckIns = (recentCheckInsResult as Result.Success).data

        val plansResult = planRepository.findAll()
        if (plansResult is Result.Error) return Result.Error(plansResult.message, plansResult.cause)
        val allPlans = (plansResult as Result.Success).data.associateBy { it.id }

        val usersMap = usersWithProfiles.associateBy { it.first.id }
        
        val arrivals = recentRawCheckIns.mapNotNull { checkIn ->
            val user = usersMap[checkIn.userId]?.first
            val plan = allPlans[checkIn.planIdAtTime]
            if (user != null && plan != null) {
                Arrival(name = user.name, plan = plan.name, timestamp = checkIn.timestamp)
            } else null
        }
        
        val revenueFlowList = mutableListOf<MonthlyRevenue>()
        for (monthsAgo in (0..6).reversed()) {
            val date = nowLocal.date.minus(monthsAgo, DateTimeUnit.MONTH)
            val monthStart = LocalDateTime(date.year, date.monthNumber, 1, 0, 0)
            val nextMonthDate = date.plus(1, DateTimeUnit.MONTH)
            val nextMonth = LocalDateTime(nextMonthDate.year, nextMonthDate.monthNumber, 1, 0, 0)
            
            val paymentsInMonthResult = paymentRepository.getPaymentsInDateRange(monthStart.toInstant(tz), nextMonth.toInstant(tz))
            if (paymentsInMonthResult is Result.Error) return Result.Error(paymentsInMonthResult.message, paymentsInMonthResult.cause)
            val paymentsInMonth = (paymentsInMonthResult as Result.Success).data
            
            val total = paymentsInMonth.sumOf { it.amount }
            revenueFlowList.add(MonthlyRevenue(date.month.name.take(3), total))
        }
        
        return Result.Success(DashboardSummary(
            totalActiveMembers = activeCount,
            totalExpiredMembers = expiredCount,
            expiringSoonCount = expiringSoonCount,
            todayCheckInsCount = todayCheckIns,
            overdueRisk = overdueRiskList.sortedBy { it.daysRemaining }.take(5),
            recentArrivals = arrivals,
            revenueFlow = revenueFlowList
        ))
    }
}
