package com.gym.frontend.ui.admin

import com.gym.shared.domain.Payment
import com.gym.shared.domain.PaymentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class PaymentHistoryItemUiModel(
    val paymentDateStr: String,
    val method: String,
    val amountStr: String,
    val expirationDateStr: String
)

sealed interface PaymentHistoryUiState {
    data object Loading : PaymentHistoryUiState
    data class Success(val payments: List<PaymentHistoryItemUiModel>) : PaymentHistoryUiState
    data class Error(val message: String) : PaymentHistoryUiState
}

sealed interface PaymentSubmitState {
    data object Idle : PaymentSubmitState
    data object Saving : PaymentSubmitState
    data object Success : PaymentSubmitState
    data class Error(val message: String) : PaymentSubmitState
}

class PaymentDialogViewModel(
    private val repository: PaymentsRepository
) {
    private val _historyState = MutableStateFlow<PaymentHistoryUiState>(PaymentHistoryUiState.Loading)
    val historyState: StateFlow<PaymentHistoryUiState> = _historyState.asStateFlow()

    private val _submitState = MutableStateFlow<PaymentSubmitState>(PaymentSubmitState.Idle)
    val submitState: StateFlow<PaymentSubmitState> = _submitState.asStateFlow()

    fun loadHistory(memberId: String, scope: CoroutineScope) {
        _historyState.value = PaymentHistoryUiState.Loading
        scope.launch {
            repository.getMemberPayments(memberId)
                .onSuccess { payments ->
                    val uiModels = payments.map { payment ->
                        val dateLocal = payment.paymentDate.toLocalDateTime(TimeZone.currentSystemDefault())
                        val expLocal = payment.expirationDate.toLocalDateTime(TimeZone.currentSystemDefault())
                        PaymentHistoryItemUiModel(
                            paymentDateStr = "${dateLocal.dayOfMonth} ${dateLocal.month.name.take(3)}, ${dateLocal.year}",
                            method = payment.method,
                            amountStr = "$${payment.amount.toInt()}",
                            expirationDateStr = "${expLocal.dayOfMonth}/${expLocal.monthNumber}/${expLocal.year}"
                        )
                    }
                    _historyState.value = PaymentHistoryUiState.Success(uiModels)
                }
                .onFailure { e ->
                    _historyState.value = PaymentHistoryUiState.Error(e.message ?: "Failed to load payment history")
                }
        }
    }

    fun createPayment(request: PaymentRequest, memberId: String, scope: CoroutineScope) {
        _submitState.value = PaymentSubmitState.Saving
        scope.launch {
            repository.createPayment(request)
                .onSuccess {
                    _submitState.value = PaymentSubmitState.Success
                    loadHistory(memberId, scope)
                }
                .onFailure { e ->
                    _submitState.value = PaymentSubmitState.Error(e.message ?: "Failed to register payment")
                }
        }
    }

    fun resetSubmitState() {
        _submitState.value = PaymentSubmitState.Idle
    }
}
