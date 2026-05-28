package com.gym.frontend.ui.admin

import com.gym.shared.domain.DashboardSummary

class DashboardRepository(private val service: DashboardService) {
    suspend fun getSummary(): Result<DashboardSummary> {
        return service.getDashboardSummary()
    }
}
