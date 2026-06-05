package com.gym.frontend.ui.auth

import com.gym.shared.domain.LoginRequest
import com.gym.shared.domain.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val role: UserRole) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(
    private val authRepository: AuthRepository
) {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(scope: CoroutineScope, request: LoginRequest) {
        _uiState.value = LoginUiState.Loading
        scope.launch {
            authRepository.login(request)
                .onSuccess { authResponse ->
                    _uiState.value = LoginUiState.Success(authResponse.user.role)
                }
                .onFailure {
                    val msg = if (request.role == UserRole.MEMBER) "Invalid email or DNI" else "Invalid credentials"
                    _uiState.value = LoginUiState.Error(msg)
                }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
