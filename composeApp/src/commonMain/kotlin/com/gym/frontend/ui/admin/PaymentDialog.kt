package com.gym.frontend.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gym.frontend.ui.shared.*
import com.gym.frontend.ui.theme.*
import com.gym.shared.domain.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentDialog(
    member: Member,
    plans: List<GymPlan>,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val scope = rememberCoroutineScope()
    val viewModel = koinInject<PaymentDialogViewModel>()

    val historyState by viewModel.historyState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    val history = (historyState as? PaymentHistoryUiState.Success)?.payments ?: emptyList()
    val isLoadingHistory = historyState is PaymentHistoryUiState.Loading
    val isSaving = submitState is PaymentSubmitState.Saving

    var showSuccess by remember { mutableStateOf(false) }

    var selectedPlanId by remember { mutableStateOf(plans.find { it.name == member.currentPlan }?.id ?: plans.firstOrNull()?.id ?: "") }
    var amount by remember { mutableStateOf(plans.find { it.id == selectedPlanId }?.price?.toString() ?: "") }
    var selectedMethod by remember { mutableStateOf("Cash") }
    val methods = listOf("Cash", "Transfer")

    // Update amount when plan or method changes
    LaunchedEffect(selectedPlanId, selectedMethod) {
        val basePrice = plans.find { it.id == selectedPlanId }?.price ?: 0.0
        val finalPrice = if (selectedMethod == "Cash") basePrice * 0.9 else basePrice
        amount = finalPrice.toInt().toString()
    }

    LaunchedEffect(member.id) {
        viewModel.loadHistory(member.id, scope)
    }

    // React to submit state
    LaunchedEffect(submitState) {
        if (submitState is PaymentSubmitState.Success) {
            showSuccess = true
            viewModel.resetSubmitState()
        }
    }


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.45f).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(32.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Payments: ${member.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Manage subscription and view transaction history", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(32.dp))

                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                    // --- REGISTER PAYMENT FORM ---
                    Column(modifier = Modifier.weight(1.2f)) {
                        if (showSuccess) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(80.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Payment Registered!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text("The member's access has been extended.", color = OnSurfaceDim, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(Modifier.height(32.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    TextButton(onClick = { showSuccess = false }) {
                                        Text("Register Another", color = OnSurfaceDim)
                                    }
                                    KineticButton(
                                        onClick = onDismiss,
                                        text = "Finish & Close",
                                        modifier = Modifier.width(180.dp).height(48.dp)
                                    )
                                }
                            }
                        } else {
                            Text("Register New Payment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            
                            Text("Subscription Plan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TealPrimary)
                            Spacer(Modifier.height(8.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                plans.forEach { plan ->
                                    val selected = selectedPlanId == plan.id
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable { selectedPlanId = plan.id },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) TealPrimary.copy(alpha = 0.1f) else (if (isDark) Level1Section else Color(0xFFF5F7F7)),
                                        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, TealPrimary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = selected, onClick = { selectedPlanId = plan.id }, colors = RadioButtonDefaults.colors(selectedColor = TealPrimary))
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(plan.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("$${plan.price.toInt()}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                            
                            Text("Payment Details", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TealPrimary)
                            Spacer(Modifier.height(12.dp))

                            ModernTextField(
                                value = amount,
                                onValueChange = { /* Read only */ },
                                placeholder = "Amount to Pay",
                                readOnly = true
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text("Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                methods.forEach { method ->
                                    val selected = selectedMethod == method
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedMethod = method },
                                        label = { Text(method) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TealPrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.weight(1f))
                            
                            KineticButton(
                                onClick = {
                                    val amtValue = amount.toDoubleOrNull() ?: 0.0
                                    if (amtValue > 0) {
                                        viewModel.createPayment(
                                            PaymentRequest(member.id, amtValue, selectedMethod),
                                            member.id,
                                            scope
                                        )
                                    }
                                },
                                text = if (isSaving) "Saving..." else "Confirm Payment",
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            )
                        }
                    }

                    // --- HISTORY LIST ---
                    Column(modifier = Modifier.weight(1.8f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(20.dp), tint = OnSurfaceDim)
                            Spacer(Modifier.width(8.dp))
                            Text("Payment History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        if (isLoadingHistory) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = TealPrimary)
                            }
                        } else if (history.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No payments registered yet.", color = OnSurfaceDim)
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                history.forEach { payment ->
                                    PaymentHistoryItem(payment, isDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryItem(payment: PaymentHistoryItemUiModel, isDark: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Level1Section else Color(0xFFF5F7F7)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(payment.paymentDateStr, fontWeight = FontWeight.Bold)
                Text("Method: ${payment.method}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(payment.amountStr, fontWeight = FontWeight.Bold, color = TealPrimary)
                Text("Expires: ${payment.expirationDateStr}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F))
            }
        }
    }
}
