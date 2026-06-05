package com.gym.frontend.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gym.frontend.ui.shared.KineticButton
import com.gym.frontend.ui.shared.ModernTextField
import com.gym.frontend.ui.theme.Level1Section
import com.gym.frontend.ui.theme.LocalIsDarkMode
import com.gym.shared.domain.*
import com.gym.frontend.ui.theme.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.History

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberFormDialog(
    member: Member? = null,
    plans: List<GymPlan>,
    onDismiss: () -> Unit,
    onSave: (MemberRequest) -> Unit,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    isSuccess: Boolean = false
) {
    val isDark = LocalIsDarkMode.current
    var name by remember { mutableStateOf(member?.name ?: "") }
    var email by remember { mutableStateOf(member?.email ?: "") }
    var phone by remember { mutableStateOf(member?.phone ?: "") }
    var dni by remember { mutableStateOf(member?.dni ?: "") }
    var selectedPlanId by remember { mutableStateOf(plans.find { it.name == member?.currentPlan }?.id ?: plans.firstOrNull()?.id ?: "") }
    var isActive by remember { mutableStateOf(member?.status?.lowercase() == "active" || member == null) }
    var paymentAmount by remember { mutableStateOf(plans.find { it.id == selectedPlanId }?.price?.toString() ?: "") }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    val paymentMethods = listOf("Cash", "Transfer")
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var dniError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

    val scope = rememberCoroutineScope()
    val paymentsRepository = remember { PaymentsRepository(PaymentsService()) }
    val membersRepository = remember { MembersRepository(MembersService()) }
    var paymentHistory by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var attendanceHistory by remember { mutableStateOf<List<CheckIn>>(emptyList()) }
    var isHistoryLoading by remember { mutableStateOf(false) }
    var isAttendanceLoading by remember { mutableStateOf(false) }

    // Update amount when plan changes
    LaunchedEffect(selectedPlanId, selectedPaymentMethod) {
        val basePrice = plans.find { it.id == selectedPlanId }?.price ?: 0.0
        val finalPrice = if (selectedPaymentMethod == "Cash"){
            basePrice * 0.9
            //"10% cash discount applied"

        } else basePrice
        paymentAmount = finalPrice.toInt().toString()
    }

    // Load history if editing
    LaunchedEffect(member?.id) {
        if (member != null) {
            isHistoryLoading = true
            paymentsRepository.getMemberPayments(member.id).onSuccess {
                paymentHistory = it
                isHistoryLoading = false
            }.onFailure {
                isHistoryLoading = false
            }
            
            isAttendanceLoading = true
            membersRepository.getAttendanceHistory(member.id).onSuccess {
                attendanceHistory = it
                isAttendanceLoading = false
            }.onFailure {
                isAttendanceLoading = false
            }
        }
    }

    // Reset saving state when result arrives
    LaunchedEffect(errorMessage, isSuccess) {
        if (errorMessage != null || isSuccess) {
            isSaving = false
        }
    }

    // Removed local PaymentDialog as it's now integrated

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (isSuccess) 0.3f else if (member == null) 0.38f else 0.75f).fillMaxHeight(if (isSuccess) 0.5f else 0.9f),
            shape = RoundedCornerShape(32.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ) {
            if (isSuccess) {
                Column(
                    modifier = Modifier.padding(32.dp).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = TealPrimary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        text = if (member == null) "Member Created!" else "Changes Saved!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = if (member == null) 
                            "The new membership has been successfully registered." 
                            else "The member information has been updated.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceDim,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    KineticButton(
                        onClick = onDismiss,
                        text = "Continue",
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )
                }
            } else {
                Column(modifier = Modifier.padding(32.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (member == null) "New Membership" else "Member Management",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (member != null) {
                                Text(member.name, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "close")
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(48.dp)
                    ) {
                        // --- LEFT COLUMN: BASIC INFO ---
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text("Basic Information", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TealPrimary)
                            
                            ModernTextField(
                                value = name,
                                onValueChange = { 
                                    name = it 
                                    nameError = null
                                    isSaving = false
                                },
                                placeholder = "Full Name",
                                errorText = nameError
                            )

                            ModernTextField(
                                value = dni,
                                onValueChange = { 
                                    dni = it 
                                    dniError = null
                                    isSaving = false
                                },
                                placeholder = "DNI / Identification",
                                errorText = dniError
                            )

                            ModernTextField(
                                value = email,
                                onValueChange = { 
                                    email = it
                                    emailError = null
                                    isSaving = false
                                    if (errorMessage?.contains("email", ignoreCase = true) == true) {
                                        onClearError()
                                    }
                                },
                                placeholder = "Email Address",
                                errorText = emailError ?: (if (errorMessage?.contains("email", ignoreCase = true) == true) errorMessage else null)
                            )

                            ModernTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                placeholder = "Phone Number (Optional)"
                            )

                            if (member == null) {
                                Spacer(Modifier.height(8.dp))
                                Text("Subscription Plan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TealPrimary)
                                
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    plans.forEach { plan ->
                                        val selected = selectedPlanId == plan.id
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().clickable { selectedPlanId = plan.id },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) TealPrimary.copy(alpha = 0.1f) else (if (isDark) Level1Section else Color(0xFFF5F7F7)),
                                            border = if (selected) BorderStroke(1.dp, TealPrimary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(selected = selected, onClick = { selectedPlanId = plan.id }, colors = RadioButtonDefaults.colors(selectedColor = TealPrimary))
                                                Spacer(Modifier.width(12.dp))
                                                Column {
                                                    Text(plan.name, fontWeight = FontWeight.Bold)
                                                    Text(plan.description ?: "", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                                                }
                                                Spacer(Modifier.weight(1f))
                                                Text("$${plan.price.toInt()}", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                Text("Initial Payment", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TealPrimary)
                                
                                ModernTextField(
                                    value = "$ $paymentAmount",
                                    onValueChange = { /* Read only */ },
                                    placeholder = "Amount to Pay",
                                    readOnly = true,
                                )

                                Text(
                                    if (selectedPaymentMethod == "Cash") " 10% cash discount applied" else "", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50)
                                )

                                Text("Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    paymentMethods.forEach { method ->
                                        val selected = selectedPaymentMethod == method
                                        FilterChip(
                                            selected = selected,
                                            onClick = { selectedPaymentMethod = method },
                                            label = { Text(method) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // --- RIGHT COLUMN: SUBSCRIPTION & PAYMENTS (Only if editing) ---
                        if (member != null) {
                            val isExpired = member.status.uppercase() != "ACTIVE"
                            var isRegisteringPayment by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier.weight(1.2f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Subscription & Payments", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TealPrimary)
                                    Surface(
                                        color = if (!isExpired) TealPrimary.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            if (!isExpired) "ACTIVE" else "EXPIRED",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isExpired) TealPrimary else Color.Red
                                        )
                                    }
                                }
                                
                                // Weekly Usage (Admin View)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    color = if (isDark) Level1Section.copy(alpha = 0.5f) else Color(0xFFF5F7F7)
                                ) {
                                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Text("Weekly Attendance", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                                            val limit = member.weeklyAttendanceLimit?.toString() ?: "Unlimited"
                                            Text("${member.weeklyAttendanceCount} / $limit visits", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (!isExpired) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (isDark) Level1Section else Color(0xFFF5F7F7)
                                    ) {
                                        Column(modifier = Modifier.padding(24.dp)) {
                                            Text("Active Plan", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                                            Text(member.currentPlan ?: "Standard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(8.dp))
                                            Text("Valid until ${member.expirationDate?.toLocalDateTime(TimeZone.currentSystemDefault())?.let { "${it.dayOfMonth}/${it.monthNumber}/${it.year}" } ?: "N/A"}", color = TealPrimary, fontWeight = FontWeight.Bold)
                                            
                                            Spacer(Modifier.height(16.dp))
                                            Text("To change the plan or renew, wait for the current period to expire.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                                        }
                                    }
                                } else {
                                    // Payment Registration Form
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        color = if (isDark) Level1Section else Color(0xFFF5F7F7),
                                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(24.dp)) {
                                            Text("Renew Membership", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(16.dp))
                                            
                                            plans.forEach { plan ->
                                                val selected = selectedPlanId == plan.id
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { selectedPlanId = plan.id },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (selected) TealPrimary.copy(alpha = 0.1f) else (if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White),
                                                    border = if (selected) BorderStroke(1.dp, TealPrimary) else null
                                                ) {
                                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        RadioButton(selected = selected, onClick = { selectedPlanId = plan.id }, colors = RadioButtonDefaults.colors(selectedColor = TealPrimary))
                                                        Column {
                                                            Text(plan.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                            Text("$${plan.price.toInt()}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(12.dp))
                                            Text("Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                paymentMethods.forEach { method ->
                                                    val selected = selectedPaymentMethod == method
                                                    FilterChip(
                                                        selected = selected,
                                                        onClick = { selectedPaymentMethod = method },
                                                        label = { Text(method) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                            
                                            Text("Total to Pay: $ $paymentAmount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TealPrimary)
                                            if (selectedPaymentMethod == "Cash") {
                                                Text("10% cash discount applied", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                                            }
                                            
                                            Spacer(Modifier.height(16.dp))
                                            KineticButton(
                                                onClick = {
                                                    val amtValue = paymentAmount.toDoubleOrNull() ?: 0.0
                                                    if (amtValue > 0) {
                                                        isRegisteringPayment = true
                                                        scope.launch {
                                                            paymentsRepository.createPayment(PaymentRequest(member.id, amtValue, selectedPaymentMethod))
                                                                .onSuccess {
                                                                    isRegisteringPayment = false
                                                                    // Reload data
                                                                    paymentsRepository.getMemberPayments(member.id).onSuccess { paymentHistory = it }
                                                                    onDismiss()
                                                                }
                                                                .onFailure { isRegisteringPayment = false }
                                                        }
                                                    }
                                                },
                                                text = "Register Payment",
                                                loading = isRegisteringPayment,
                                                modifier = Modifier.fillMaxWidth().height(48.dp)
                                            )
                                        }
                                    }
                                }

                                // History
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = OnSurfaceDim)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Recent Payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                if (isHistoryLoading) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = TealPrimary)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        for (payment in paymentHistory.take(3)) {
                                            val dateL = payment.paymentDate.toLocalDateTime(TimeZone.currentSystemDefault())
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isDark) Level1Section.copy(alpha = 0.5f) else Color(0xFFF5F7F7)
                                            ) {
                                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column {
                                                        Text("${dateL.dayOfMonth} ${dateL.month.name.take(3)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                        Text(payment.method, style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                                                    }
                                                    Text("$${payment.amount.toInt()}", fontWeight = FontWeight.Bold, color = TealPrimary, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                        if (paymentHistory.size > 3) {
                                            Text("See all history in the payments section...", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                                        }
                                    }
                                }

                                // Attendance History
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = OnSurfaceDim)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Recent Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                if (isAttendanceLoading) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = TealPrimary)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (attendanceHistory.isEmpty()) {
                                            Text("No attendance logs found", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                                        } else {
                                            attendanceHistory.take(5).forEach { log ->
                                                val dateL = log.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (isDark) Level1Section.copy(alpha = 0.5f) else Color(0xFFF5F7F7)
                                                ) {
                                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = TealPrimary)
                                                        Spacer(Modifier.width(12.dp))
                                                        Column {
                                                            Text("${dateL.dayOfMonth} ${dateL.month.name.take(3)}, ${dateL.year}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                            Text("${dateL.hour.toString().padStart(2, '0')}:${dateL.minute.toString().padStart(2, '0')} hs", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Cancel", color = if (isDark) Color.White else Color.Black)
                        }
                        KineticButton(
                            onClick = { 
                                if (isSaving) return@KineticButton
                                
                                // Reset local errors
                                nameError = null
                                emailError = null
                                dniError = null
                                
                                // Clear server error
                                onClearError()

                                if (name.isBlank()) {
                                    nameError = "Name is required"
                                    return@KineticButton
                                }
                                if (dni.isBlank()) {
                                    dniError = "DNI is required"
                                    return@KineticButton
                                }
                                if (!emailRegex.matches(email)) {
                                    emailError = "Please enter a valid email address"
                                    return@KineticButton
                                }
                                
                                isSaving = true
                                onSave(MemberRequest(
                                    name = name, 
                                    email = email, 
                                    dni = dni,
                                    phone = phone, 
                                    planId = selectedPlanId, 
                                    isActive = isActive,
                                    paymentAmount = paymentAmount.toDoubleOrNull() ?: 0.0,
                                    paymentMethod = selectedPaymentMethod
                                ))
                            },
                            text = if (member == null) "Create & Pay" else "Save Changes",
                            loading = isSaving,
                            modifier = Modifier.weight(1f).height(56.dp)
                        )
                    }
                    
                    if (errorMessage != null && errorMessage?.contains("email", ignoreCase = true) == false) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
