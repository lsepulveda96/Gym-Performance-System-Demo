package com.gym.server.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class DashboardSummaryResponse(
    val totalActiveMembers: Int,
    val totalExpiredMembers: Int,
    val expiringSoonCount: Int,
    val todayCheckInsCount: Int,
    val overdueRisk: List<RiskMemberResponse>,
    val recentArrivals: List<ArrivalResponse>,
    val revenueFlow: List<MonthlyRevenueResponse>
)
