package com.gym.frontend.ui.member

import com.gym.frontend.ui.auth.AuthRepository
import com.gym.frontend.ui.admin.MembersRepository
import com.gym.shared.domain.Member
import com.gym.shared.domain.CheckIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ActivityItemUiModel(val timeStr: String, val dateStr: String, val isPrimaryColor: Boolean)

data class MemberHomeUiModel(
    val userName: String,
    val planName: String,
    val statusText: String,
    val isStatusActive: Boolean,
    val renewalProgress: Float,
    val remainingDays: Int,
    val weeklyAttendanceCount: Int,
    val weeklyAttendanceLimitText: String,
    val isWeeklyLimitReached: Boolean,
    val attendanceHistory: List<ActivityItemUiModel>
)

sealed interface MemberHomeUiState {
    data object Loading : MemberHomeUiState
    data class Success(val uiModel: MemberHomeUiModel) : MemberHomeUiState
    data class Error(val message: String) : MemberHomeUiState
}

class MemberHomeViewModel(
    private val authRepository: AuthRepository,
    private val membersRepository: MembersRepository
) {
    private val _uiState = MutableStateFlow<MemberHomeUiState>(MemberHomeUiState.Loading)
    val uiState: StateFlow<MemberHomeUiState> = _uiState.asStateFlow()

    fun loadData(scope: CoroutineScope) {
        _uiState.value = MemberHomeUiState.Loading
        scope.launch {
            authRepository.getMe().onSuccess { m ->
                val userName = m.name ?: authRepository.getUserName() ?: "Member"
                membersRepository.getAttendanceHistory(m.id).onSuccess { logs ->
                    val now = Clock.System.now()
                    val exp = m.expirationDate ?: now
                    val remainingDays = (exp.toEpochMilliseconds() - now.toEpochMilliseconds()) / (1000 * 60 * 60 * 24)
                    val daysInt = remainingDays.toInt().coerceAtLeast(0)
                    val renewalProgress = if (daysInt > 30) 1f else daysInt / 30f

                    val historyUiModels = logs.take(2).mapIndexed { index, checkIn ->
                        val dateL = checkIn.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                        val dateStr = "${dateL.dayOfMonth} ${dateL.month.name.take(3)}"
                        val timeStr = "${dateL.hour.toString().padStart(2, '0')}:${dateL.minute.toString().padStart(2, '0')}"
                        ActivityItemUiModel(timeStr, dateStr, index == 0)
                    }

                    val limitStr = m.weeklyAttendanceLimit?.let { "/ $it" } ?: ""
                    val limit = m.weeklyAttendanceLimit
                    val isLimitReached = limit != null && m.weeklyAttendanceCount >= limit

                    val uiModel = MemberHomeUiModel(
                        userName = userName,
                        planName = m.currentPlan ?: "No plan",
                        statusText = if (m.status == "Active") "Active" else "Expired",
                        isStatusActive = m.status == "Active",
                        renewalProgress = renewalProgress,
                        remainingDays = daysInt,
                        weeklyAttendanceCount = m.weeklyAttendanceCount,
                        weeklyAttendanceLimitText = limitStr,
                        isWeeklyLimitReached = isLimitReached,
                        attendanceHistory = historyUiModels
                    )

                    _uiState.value = MemberHomeUiState.Success(uiModel)
                }.onFailure { e ->
                    _uiState.value = MemberHomeUiState.Error(e.message ?: "Failed to load attendance")
                }
            }.onFailure { e ->
                _uiState.value = MemberHomeUiState.Error(e.message ?: "Failed to load member profile")
            }
        }
    }
}
