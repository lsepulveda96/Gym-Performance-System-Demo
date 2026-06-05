package com.gym.frontend.ui.admin

import com.gym.shared.domain.DashboardSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

import kotlinx.datetime.Clock

data class RiskUiModel(val name: String, val details: String, val amountStr: String)
data class ArrivalUiModel(val name: String, val plan: String, val relativeTime: String)

data class AdminDashboardUiModel(
    val activeMembers: String,
    val expiredMembers: String,
    val expiringSoon: String,
    val todayCheckIns: String,
    val overdueRisk: List<RiskUiModel>,
    val recentArrivals: List<ArrivalUiModel>,
    val revenueFlow: List<com.gym.shared.domain.MonthlyRevenue>
)

sealed interface AdminDashboardUiState {
    data object Loading : AdminDashboardUiState
    data class Success(val uiModel: AdminDashboardUiModel) : AdminDashboardUiState
    data class Error(val message: String) : AdminDashboardUiState
}

class AdminDashboardViewModel(
    private val repository: DashboardRepository
) {
    private val _uiState = MutableStateFlow<AdminDashboardUiState>(AdminDashboardUiState.Loading)
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    fun loadData(scope: CoroutineScope) {
        _uiState.value = AdminDashboardUiState.Loading
        scope.launch {
            repository.getSummary()
                .onSuccess { summary ->
                    val uiModel = AdminDashboardUiModel(
                        activeMembers = summary.totalActiveMembers.toString(),
                        expiredMembers = summary.totalExpiredMembers.toString(),
                        expiringSoon = summary.expiringSoonCount.toString(),
                        todayCheckIns = summary.todayCheckInsCount.toString(),
                        overdueRisk = summary.overdueRisk.map { 
                            RiskUiModel(
                                name = it.name,
                                details = "Expires in ${it.daysRemaining} days",
                                amountStr = "$${it.amount.toInt()}"
                            )
                        },
                        recentArrivals = summary.recentArrivals.map { arrival ->
                            val now = Clock.System.now()
                            val diff = now.minus(arrival.timestamp).inWholeMinutes
                            val timeStr = when {
                                diff < 1 -> "JUST NOW"
                                diff < 60 -> "${diff}M AGO"
                                else -> "${diff / 60}H AGO"
                            }
                            ArrivalUiModel(
                                name = arrival.name,
                                plan = arrival.plan,
                                relativeTime = timeStr
                            )
                        },
                        revenueFlow = summary.revenueFlow
                    )
                    _uiState.value = AdminDashboardUiState.Success(uiModel)
                }
                .onFailure { error ->
                    _uiState.value = AdminDashboardUiState.Error(error.message ?: "Failed to load dashboard metrics")
                }
        }
    }
}
