package com.gym.frontend.ui.member

import com.gym.frontend.ui.admin.MembersRepository
import com.gym.frontend.ui.admin.PaymentsRepository
import com.gym.frontend.ui.auth.AuthRepository
import com.gym.shared.domain.CheckIn
import com.gym.shared.domain.Member
import com.gym.shared.domain.Payment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val member: Member,
        val payments: List<Payment> = emptyList(),
        val attendanceHistory: List<CheckIn> = emptyList(),
        val isPaymentsLoading: Boolean = true,
        val isAttendanceLoading: Boolean = true
    ) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val membersRepository: MembersRepository,
    private val paymentsRepository: PaymentsRepository
) {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadData(scope: CoroutineScope) {
        _uiState.value = ProfileUiState.Loading
        scope.launch {
            authRepository.getMe()
                .onSuccess { member ->
                    // Set base success state, sub-lists still loading
                    _uiState.value = ProfileUiState.Success(
                        member = member,
                        isPaymentsLoading = true,
                        isAttendanceLoading = true
                    )

                    // Load attendance in parallel
                    launch {
                        membersRepository.getAttendanceHistory(member.id)
                            .onSuccess { logs ->
                                val current = _uiState.value as? ProfileUiState.Success ?: return@launch
                                _uiState.value = current.copy(
                                    attendanceHistory = logs,
                                    isAttendanceLoading = false
                                )
                            }
                            .onFailure {
                                val current = _uiState.value as? ProfileUiState.Success ?: return@launch
                                _uiState.value = current.copy(isAttendanceLoading = false)
                            }
                    }

                    // Load payments in parallel
                    launch {
                        paymentsRepository.getMemberPayments(member.id)
                            .onSuccess { paymentList ->
                                val current = _uiState.value as? ProfileUiState.Success ?: return@launch
                                _uiState.value = current.copy(
                                    payments = paymentList,
                                    isPaymentsLoading = false
                                )
                            }
                            .onFailure {
                                val current = _uiState.value as? ProfileUiState.Success ?: return@launch
                                _uiState.value = current.copy(isPaymentsLoading = false)
                            }
                    }
                }
                .onFailure { e ->
                    _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
                }
        }
    }

    fun updateProfileImage(avatarName: String, scope: CoroutineScope) {
        scope.launch {
            authRepository.updateProfileImage(avatarName).onSuccess {
                val current = _uiState.value as? ProfileUiState.Success ?: return@launch
                _uiState.value = current.copy(member = current.member.copy(profileImageUrl = avatarName))
            }
        }
    }
}
