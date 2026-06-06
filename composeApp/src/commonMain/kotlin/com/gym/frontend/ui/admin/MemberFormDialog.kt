package com.gym.frontend.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gym.frontend.ui.shared.KineticButton
import com.gym.frontend.ui.shared.ModernTextField
import com.gym.frontend.ui.theme.*
import com.gym.shared.domain.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
        val finalPrice = if (selectedPaymentMethod == "Cash") {
            basePrice * 0.9
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isSuccess) 0.3f else if (member == null) 0.38f else 0.75f)
                .fillMaxHeight(if (isSuccess) 0.5f else 0.9f),
            shape = RoundedCornerShape(32.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ) {
            if (isSuccess) {
                MemberFormSuccess(
                    isNewMember = member == null,
                    onContinue = onDismiss
                )
            } else {
                MemberFormContent(
                    member = member,
                    plans = plans,
                    name = name,
                    onNameChange = { name = it; nameError = null; isSaving = false },
                    nameError = nameError,
                    dni = dni,
                    onDniChange = { dni = it; dniError = null; isSaving = false },
                    dniError = dniError,
                    email = email,
                    onEmailChange = {
                        email = it
                        emailError = null
                        isSaving = false
                        if (errorMessage?.contains("email", ignoreCase = true) == true) {
                            onClearError()
                        }
                    },
                    emailError = emailError ?: (if (errorMessage?.contains("email", ignoreCase = true) == true) errorMessage else null),
                    phone = phone,
                    onPhoneChange = { phone = it },
                    selectedPlanId = selectedPlanId,
                    onPlanSelect = { selectedPlanId = it },
                    paymentAmount = paymentAmount,
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodSelect = { selectedPaymentMethod = it },
                    paymentMethods = paymentMethods,
                    paymentHistory = paymentHistory,
                    isHistoryLoading = isHistoryLoading,
                    attendanceHistory = attendanceHistory,
                    isAttendanceLoading = isAttendanceLoading,
                    isSaving = isSaving,
                    errorMessage = if (errorMessage?.contains("email", ignoreCase = true) == false) errorMessage else null,
                    onDismiss = onDismiss,
                    onSave = {
                        // Reset local errors
                        nameError = null
                        emailError = null
                        dniError = null
                        onClearError()

                        if (name.isBlank()) {
                            nameError = "Name is required"
                        } else if (dni.isBlank()) {
                            dniError = "DNI is required"
                        } else if (!emailRegex.matches(email)) {
                            emailError = "Please enter a valid email address"
                        } else {
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
                        }
                    },
                    onRegisterPayment = { amount, method ->
                        scope.launch {
                            paymentsRepository.createPayment(PaymentRequest(member!!.id, amount, method))
                                .onSuccess {
                                    paymentsRepository.getMemberPayments(member.id).onSuccess { paymentHistory = it }
                                    onDismiss()
                                }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MemberFormSuccess(
    isNewMember: Boolean,
    onContinue: () -> Unit
) {
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
            text = if (isNewMember) "Member Created!" else "Changes Saved!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isNewMember)
                "The new membership has been successfully registered."
            else "The member information has been updated.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceDim,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        KineticButton(
            onClick = onContinue,
            text = "Continue",
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
    }
}

@Composable
fun MemberFormContent(
    member: Member?,
    plans: List<GymPlan>,
    name: String,
    onNameChange: (String) -> Unit,
    nameError: String?,
    dni: String,
    onDniChange: (String) -> Unit,
    dniError: String?,
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    phone: String,
    onPhoneChange: (String) -> Unit,
    selectedPlanId: String,
    onPlanSelect: (String) -> Unit,
    paymentAmount: String,
    selectedPaymentMethod: String,
    onPaymentMethodSelect: (String) -> Unit,
    paymentMethods: List<String>,
    paymentHistory: List<Payment>,
    isHistoryLoading: Boolean,
    attendanceHistory: List<CheckIn>,
    isAttendanceLoading: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onRegisterPayment: (Double, String) -> Unit
) {
    val isDark = LocalIsDarkMode.current

    Column(modifier = Modifier.padding(32.dp)) {
        MemberFormHeader(member = member, onDismiss = onDismiss)

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            // LEFT COLUMN: BASIC INFO
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                MemberBasicInfoSection(
                    name = name,
                    onNameChange = onNameChange,
                    nameError = nameError,
                    dni = dni,
                    onDniChange = onDniChange,
                    dniError = dniError,
                    email = email,
                    onEmailChange = onEmailChange,
                    emailError = emailError,
                    phone = phone,
                    onPhoneChange = onPhoneChange
                )

                if (member == null) {
                    MemberInitialSubscriptionSection(
                        plans = plans,
                        selectedPlanId = selectedPlanId,
                        onPlanSelect = onPlanSelect,
                        paymentAmount = paymentAmount,
                        selectedPaymentMethod = selectedPaymentMethod,
                        onPaymentMethodSelect = onPaymentMethodSelect,
                        paymentMethods = paymentMethods
                    )
                }
            }

            // RIGHT COLUMN: SUBSCRIPTION & PAYMENTS (Only if editing)
            if (member != null) {
                MemberDetailsSidePanel(
                    member = member,
                    plans = plans,
                    selectedPlanId = selectedPlanId,
                    onPlanSelect = onPlanSelect,
                    paymentAmount = paymentAmount,
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodSelect = onPaymentMethodSelect,
                    paymentMethods = paymentMethods,
                    paymentHistory = paymentHistory,
                    isHistoryLoading = isHistoryLoading,
                    attendanceHistory = attendanceHistory,
                    isAttendanceLoading = isAttendanceLoading,
                    onRegisterPayment = onRegisterPayment
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        MemberFormFooter(
            member = member,
            isSaving = isSaving,
            errorMessage = errorMessage,
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

@Composable
private fun MemberFormHeader(member: Member?, onDismiss: () -> Unit) {
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
}

@Composable
private fun MemberBasicInfoSection(
    name: String,
    onNameChange: (String) -> Unit,
    nameError: String?,
    dni: String,
    onDniChange: (String) -> Unit,
    dniError: String?,
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    Text("Basic Information", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TealPrimary)

    ModernTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = "Full Name",
        errorText = nameError
    )

    ModernTextField(
        value = dni,
        onValueChange = onDniChange,
        placeholder = "DNI / Identification",
        errorText = dniError
    )

    ModernTextField(
        value = email,
        onValueChange = onEmailChange,
        placeholder = "Email Address",
        errorText = emailError
    )

    ModernTextField(
        value = phone,
        onValueChange = onPhoneChange,
        placeholder = "Phone Number (Optional)"
    )
}

@Composable
private fun MemberInitialSubscriptionSection(
    plans: List<GymPlan>,
    selectedPlanId: String,
    onPlanSelect: (String) -> Unit,
    paymentAmount: String,
    selectedPaymentMethod: String,
    onPaymentMethodSelect: (String) -> Unit,
    paymentMethods: List<String>
) {
    Spacer(Modifier.height(8.dp))
    Text("Subscription Plan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TealPrimary)

    MemberPlanSelection(
        plans = plans,
        selectedPlanId = selectedPlanId,
        onPlanSelect = onPlanSelect
    )

    Spacer(Modifier.height(16.dp))
    Text("Initial Payment", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TealPrimary)

    ModernTextField(
        value = "$ $paymentAmount",
        onValueChange = { /* Read only */ },
        placeholder = "Amount to Pay",
        readOnly = true,
    )

    if (selectedPaymentMethod == "Cash") {
        Text(" 10% cash discount applied", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
    }

    Text("Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    MemberPaymentMethodSelection(
        selectedPaymentMethod = selectedPaymentMethod,
        onPaymentMethodSelect = onPaymentMethodSelect,
        paymentMethods = paymentMethods
    )
}

@Composable
private fun MemberPlanSelection(
    plans: List<GymPlan>,
    selectedPlanId: String,
    onPlanSelect: (String) -> Unit
) {
    val isDark = LocalIsDarkMode.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        plans.forEach { plan ->
            val selected = selectedPlanId == plan.id
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onPlanSelect(plan.id) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) TealPrimary.copy(alpha = 0.1f) else (if (isDark) Level1Section else Color(0xFFF5F7F7)),
                border = if (selected) BorderStroke(1.dp, TealPrimary) else null
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected, onClick = { onPlanSelect(plan.id) }, colors = RadioButtonDefaults.colors(selectedColor = TealPrimary))
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
}

@Composable
private fun MemberPaymentMethodSelection(
    selectedPaymentMethod: String,
    onPaymentMethodSelect: (String) -> Unit,
    paymentMethods: List<String>
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        paymentMethods.forEach { method ->
            val selected = selectedPaymentMethod == method
            FilterChip(
                selected = selected,
                onClick = { onPaymentMethodSelect(method) },
                label = { Text(method) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RowScope.MemberDetailsSidePanel(
    member: Member,
    plans: List<GymPlan>,
    selectedPlanId: String,
    onPlanSelect: (String) -> Unit,
    paymentAmount: String,
    selectedPaymentMethod: String,
    onPaymentMethodSelect: (String) -> Unit,
    paymentMethods: List<String>,
    paymentHistory: List<Payment>,
    isHistoryLoading: Boolean,
    attendanceHistory: List<CheckIn>,
    isAttendanceLoading: Boolean,
    onRegisterPayment: (Double, String) -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val isExpired = member.status.uppercase() != "ACTIVE"

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
            StatusBadge(isActive = !isExpired)
        }

        // Weekly Usage
        AttendanceSummaryCard(member = member)

        if (!isExpired) {
            ActivePlanCard(member = member)
        } else {
            RenewalForm(
                plans = plans,
                selectedPlanId = selectedPlanId,
                onPlanSelect = onPlanSelect,
                paymentAmount = paymentAmount,
                selectedPaymentMethod = selectedPaymentMethod,
                onPaymentMethodSelect = onPaymentMethodSelect,
                paymentMethods = paymentMethods,
                onRegisterPayment = { onRegisterPayment(paymentAmount.toDoubleOrNull() ?: 0.0, selectedPaymentMethod) }
            )
        }

        // Recent Payments
        HistorySection(
            title = "Recent Payments",
            icon = Icons.Outlined.History,
            isLoading = isHistoryLoading
        ) {
            PaymentHistoryList(paymentHistory = paymentHistory)
        }

        // Recent Attendance
        HistorySection(
            title = "Recent Attendance",
            icon = Icons.Outlined.History,
            isLoading = isAttendanceLoading
        ) {
            AttendanceHistoryList(attendanceHistory = attendanceHistory)
        }
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    Surface(
        color = if (isActive) TealPrimary.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            if (isActive) "ACTIVE" else "EXPIRED",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isActive) TealPrimary else Color.Red
        )
    }
}

@Composable
private fun AttendanceSummaryCard(member: Member) {
    val isDark = LocalIsDarkMode.current
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
}

@Composable
private fun ActivePlanCard(member: Member) {
    val isDark = LocalIsDarkMode.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) Level1Section else Color(0xFFF5F7F7)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Active Plan", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
            Text(member.currentPlan ?: "Standard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Valid until ${member.expirationDate?.toLocalDateTime(TimeZone.currentSystemDefault())?.let { "${it.dayOfMonth}/${it.monthNumber}/${it.year}" } ?: "N/A"}",
                color = TealPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))
            Text("To change the plan or renew, wait for the current period to expire.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
        }
    }
}

@Composable
private fun RenewalForm(
    plans: List<GymPlan>,
    selectedPlanId: String,
    onPlanSelect: (String) -> Unit,
    paymentAmount: String,
    selectedPaymentMethod: String,
    onPaymentMethodSelect: (String) -> Unit,
    paymentMethods: List<String>,
    onRegisterPayment: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    var isRegisteringPayment by remember { mutableStateOf(false) }

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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onPlanSelect(plan.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) TealPrimary.copy(alpha = 0.1f) else (if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White),
                    border = if (selected) BorderStroke(1.dp, TealPrimary) else null
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected, onClick = { onPlanSelect(plan.id) }, colors = RadioButtonDefaults.colors(selectedColor = TealPrimary))
                        Column {
                            Text(plan.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("$${plan.price.toInt()}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            MemberPaymentMethodSelection(
                selectedPaymentMethod = selectedPaymentMethod,
                onPaymentMethodSelect = onPaymentMethodSelect,
                paymentMethods = paymentMethods
            )

            Spacer(Modifier.height(8.dp))
            Text("Total to Pay: $ $paymentAmount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TealPrimary)
            if (selectedPaymentMethod == "Cash") {
                Text("10% cash discount applied", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
            }

            Spacer(Modifier.height(16.dp))
            KineticButton(
                onClick = {
                    isRegisteringPayment = true
                    onRegisterPayment()
                },
                text = "Register Payment",
                loading = isRegisteringPayment,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }
    }
}

@Composable
private fun HistorySection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean,
    content: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = OnSurfaceDim)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }

    if (isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = TealPrimary)
    } else {
        content()
    }
}

@Composable
private fun PaymentHistoryList(paymentHistory: List<Payment>) {
    val isDark = LocalIsDarkMode.current
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

@Composable
private fun AttendanceHistoryList(attendanceHistory: List<CheckIn>) {
    val isDark = LocalIsDarkMode.current
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

@Composable
private fun MemberFormFooter(
    member: Member?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel", color = if (isDark) Color.White else Color.Black)
            }
            KineticButton(
                onClick = onSave,
                text = if (member == null) "Create & Pay" else "Save Changes",
                loading = isSaving,
                modifier = Modifier.weight(1f).height(56.dp)
            )
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
