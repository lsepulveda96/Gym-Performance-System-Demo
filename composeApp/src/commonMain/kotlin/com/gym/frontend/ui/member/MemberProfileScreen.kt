package com.gym.frontend.ui.member

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gym.frontend.ui.theme.*
import com.gym.frontend.ui.shared.*

import com.gym.frontend.ui.auth.*
import com.gym.shared.domain.*
import com.gym.frontend.ui.admin.MembersRepository
import com.gym.frontend.ui.admin.MembersService
import com.gym.frontend.ui.admin.PaymentsService
import com.gym.frontend.ui.admin.PaymentsRepository
import androidx.compose.runtime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import gym_system.composeapp.generated.resources.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items

@Composable
fun ProfileScreen(onLogout: () -> Unit, isDark: Boolean, onToggleDark: () -> Unit) {
    val scope = rememberCoroutineScope()
    val viewModel = remember {
        ProfileViewModel(
            authRepository = AuthRepository(AuthService(), TokenManager()),
            membersRepository = MembersRepository(MembersService()),
            paymentsRepository = PaymentsRepository(PaymentsService())
        )
    }
    val uiState by viewModel.uiState.collectAsState()

    val successState = uiState as? ProfileUiState.Success
    val member = successState?.member
    val attendanceHistory = successState?.attendanceHistory ?: emptyList()
    val payments = successState?.payments ?: emptyList()
    val isPaymentsLoading = successState?.isPaymentsLoading ?: false
    val isAttendanceLoading = successState?.isAttendanceLoading ?: false

    LaunchedEffect(Unit) {
        viewModel.loadData(scope)
    }

    if (uiState is ProfileUiState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TealPrimary)
        }
        return
    }

    if (uiState is ProfileUiState.Error) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text((uiState as ProfileUiState.Error).message, color = OnSurfaceDim)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.loadData(scope) }) { Text("Retry") }
            }
        }
        return
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = "Profile",
            onActionClick = {}
        )

        // --- PROFILE IMAGE & NAME ---
        var showAvatarPicker by remember { mutableStateOf(false) }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showAvatarPicker = true },
                contentAlignment = Alignment.Center
            ) {
                val avatarRes = getAvatarResource(member?.profileImageUrl)
                if (avatarRes != null) {
                    Image(
                        painter = painterResource(avatarRes),
                        contentDescription = "Profile Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = OnSurfaceDim)
                }
                
                // Edit Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column {
                Text(member?.name ?: "User Name", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(member?.email ?: "user@example.com", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
            }
        }

        if (showAvatarPicker) {
            AvatarSelectionDialog(
                onDismiss = { showAvatarPicker = false },
                onAvatarSelected = { avatarName ->
                    showAvatarPicker = false
                    viewModel.updateProfileImage(avatarName, scope)
                }
            )
        }


        // --- CURRENT PLAN CARD (Teal Gradient) ---
        val expDateStr = member?.expirationDate?.let {
            val local = it.toLocalDateTime(TimeZone.currentSystemDefault())
            "${local.dayOfMonth} ${local.month.name.take(3)}, ${local.year}"
        } ?: "No payment"

        val joinDateStr = member?.joinDate?.let {
            val local = it.toLocalDateTime(TimeZone.currentSystemDefault())
            "${local.month.name.take(3)} ${local.year}"
        } ?: "Jan 2022"

        Surface(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(TealPrimary, Color(0xFF104A4B))))
                .padding(32.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("CURRENT PLAN", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text(member?.currentPlan ?: "Atelier Member", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("Membership Expiration", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(expDateStr, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Status", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Text(member?.status?.uppercase() ?: "UNKNOWN", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // Badge Icon
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).align(Alignment.TopEnd), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = Color.White)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        // --- WEEKLY ATTENDANCE ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(TealPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = TealPrimary)
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Visits this week", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim, fontWeight = FontWeight.Bold)
                    val limitStr = member?.weeklyAttendanceLimit?.toString() ?: "Unlimited"
                    Text("${member?.weeklyAttendanceCount ?: 0} / $limitStr", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                
                // Progress Indicator
                member?.weeklyAttendanceLimit?.let { limit ->
                    val progress = (member?.weeklyAttendanceCount ?: 0).toFloat() / limit
                    CircularProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier.size(32.dp),
                        color = if (progress >= 1f) Color(0xFFD32F2F) else TealPrimary,
                        strokeWidth = 4.dp,
                        trackColor = TealPrimary.copy(alpha = 0.1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- STATUS ROW ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusMiniCard("Account Status", member?.status ?: "Unknown", Icons.Filled.CheckCircle, Modifier.weight(1f))
            StatusMiniCard("Member Since", joinDateStr, Icons.Outlined.History, Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        // --- PAYMENT METHOD ---
        Text("Payment Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = TealPrimary)
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("•••• •••• •••• 4242", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Expires 12/26", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                }
                Text("Update", color = TealPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- PAYMENT HISTORY ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Payment History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (isPaymentsLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TealPrimary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (payments.isEmpty() && !isPaymentsLoading) {
                Text("No payments found", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
            } else {
                payments.take(5).forEach { payment ->
                    val dateL = payment.paymentDate.toLocalDateTime(TimeZone.currentSystemDefault())
                    val dateStr = "${dateL.dayOfMonth} ${dateL.month.name.take(3)}, ${dateL.year}"
                    val intPart = payment.amount.toInt()
                    val fracPart = ((payment.amount - intPart) * 100).toInt()
                    val amountStr = "$${intPart}.${fracPart.toString().padStart(2, '0')}"
                    HistoryItem("${payment.method} Payment", dateStr, amountStr, "PAID")
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- ATTENDANCE HISTORY ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Last Visits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (isAttendanceLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TealPrimary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (attendanceHistory.isEmpty() && !isAttendanceLoading) {
                Text("No recent visits found", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
            } else {
                attendanceHistory.take(5).forEach { log ->
                    val dateL = log.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(TealPrimary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${dateL.dayOfMonth} ${dateL.month.name.take(3)}, ${dateL.year}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("Check-in successful", color = OnSurfaceDim, style = MaterialTheme.typography.labelSmall)
                            }
                            Text("${dateL.hour.toString().padStart(2, '0')}:${dateL.minute.toString().padStart(2, '0')}", fontWeight = FontWeight.Bold, color = TealPrimary)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- DOWNLOAD TAX STATEMENT ---
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = OnSurfaceNeutral)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Receipt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Download Tax Statement (2023)", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(32.dp))
        
        // --- APP SETTINGS ---
        Text("APP SETTINGS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = OnSurfaceNeutral)
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White), contentAlignment = Alignment.Center) {
                         Icon(if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Text("Dark Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = isDark,
                    onCheckedChange = { onToggleDark() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TealPrimary
                    )
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        // --- LOG OUT ---
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF2C1B1B) else Color(0xFFFFF3F1),
                contentColor = Color(0xFFD32F2F)
            )
        ) {
            Text("Log Out Account", fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun HistoryItem(title: String, date: String, amount: String, status: String) {
    val isDark = LocalIsDarkMode.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (isDark) Color(0xFF1E2829) else Color(0xFFF0F4F4)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Receipt, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(date, color = OnSurfaceDim, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(status, color = TealPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AvatarSelectionDialog(
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit
) {
    val avatars = listOf(
        "avatar_cardio", "avatar_fuel", "avatar_paz", "avatar_pose", 
        "avatar_running", "avatar_strength", "avatar_triumph", "avatar_weight"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select your Avatar", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Choose an image that represents your kinetic energy today.", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(320.dp)
                ) {
                    items(avatars) { avatarName ->
                        Surface(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onAvatarSelected(avatarName) },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            val res = getAvatarResource(avatarName)
                            if (res != null) {
                                Image(
                                    painter = painterResource(res),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceDim) }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

