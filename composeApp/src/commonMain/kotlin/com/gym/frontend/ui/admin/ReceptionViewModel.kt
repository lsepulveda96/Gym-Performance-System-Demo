package com.gym.frontend.ui.admin

import com.gym.frontend.ui.member.AccessService
import com.gym.shared.domain.AccessValidationResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReceptionUiState {
    data object Idle : ReceptionUiState
    data object Validating : ReceptionUiState
    data class Validated(val result: AccessValidationResponse) : ReceptionUiState
    data class Error(val message: String) : ReceptionUiState
}

class ReceptionViewModel(
    private val accessService: AccessService
) {
    private val _uiState = MutableStateFlow<ReceptionUiState>(ReceptionUiState.Idle)
    val uiState: StateFlow<ReceptionUiState> = _uiState.asStateFlow()

    private var lastScannedCode: String? = null

    fun validateCode(code: String, fromCamera: Boolean = false, scope: CoroutineScope) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return

        // Debounce: skip if same code already validated from camera
        if (fromCamera && trimmed == lastScannedCode && _uiState.value is ReceptionUiState.Validated) return

        _uiState.value = ReceptionUiState.Validating
        scope.launch {
            accessService.validateAccess(trimmed)
                .onSuccess { response ->
                    lastScannedCode = trimmed
                    _uiState.value = ReceptionUiState.Validated(response)
                }
                .onFailure { e ->
                    lastScannedCode = trimmed
                    _uiState.value = ReceptionUiState.Validated(
                        AccessValidationResponse(
                            success = false,
                            message = e.message ?: "Validation failed"
                        )
                    )
                }
        }
    }

    fun clearResult() {
        lastScannedCode = null
        _uiState.value = ReceptionUiState.Idle
    }
}
