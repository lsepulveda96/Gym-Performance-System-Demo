package com.gym.frontend.ui.member

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.frontend.ui.theme.*
import com.gym.frontend.ui.shared.*
import com.gym.frontend.ui.auth.*
import com.gym.frontend.ui.admin.MembersService
import com.gym.frontend.ui.admin.MembersRepository

import org.jetbrains.compose.ui.tooling.preview.Preview

import org.koin.compose.koinInject

@Composable
fun MemberHomeScreen(onNavigate: (String) -> Unit) {
    val viewModel = koinInject<MemberHomeViewModel>()
    
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadData(scope)
    }

    MemberHomeContent(
        state = state,
        onNavigate = onNavigate
    )
}

@Composable
fun MemberHomeContent(
    state: MemberHomeUiState,
    onNavigate: (String) -> Unit
) {
    when (state) {
        is MemberHomeUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
        }
        is MemberHomeUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onNavigate("login") }) {
                        Text("Go to Login")
                    }
                }
            }
        }
        is MemberHomeUiState.Success -> {
            val uiModel = state.uiModel
            MemberHomeContent(
                uiModel = uiModel,
                onNavigate = onNavigate,
                onLogout = { onNavigate("login") }
            )
        }
    }
}

@Composable
fun MemberHomeContent(
    uiModel: MemberHomeUiModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScreenHeader(
                    title = "Home",
                    subtitle = "Hello, ${uiModel.userName}"
                )
            }
            
            Spacer(Modifier.height(24.dp))

            // --- MOTIVATIONAL QUOTE ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "\"The only bad workout is the one that didn't happen. Push past the kinetic barrier today.\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("DAILY MOMENTUM", style = MaterialTheme.typography.labelLarge, color = TealPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- CHECK-IN NOW ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f)),
                onClick = { onNavigate("checkin") }
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF1A2627)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.QrCode, contentDescription = "Qr code")
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Check In Now", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Scan to unlock your session", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- PLAN STATUS ---
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
                    Column {
                        Text("YOUR PLAN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = OnSurfaceDim)
                        Text(uiModel.planName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = LavenderTertiary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(uiModel.statusText, style = MaterialTheme.typography.bodyMedium, color = if (uiModel.isStatusActive) TealPrimary else Color(0xFFD32F2F))
                        }
                    }
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF232A2B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- RENEWAL & ACTIVITY ROW ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Renewal Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RENEWAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = OnSurfaceDim)
                        Spacer(Modifier.height(16.dp))
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { uiModel.renewalProgress },
                                modifier = Modifier.size(64.dp),
                                color = TealPrimary,
                                strokeWidth = 6.dp,
                                trackColor = Color.DarkGray
                            )
                            Text("${uiModel.remainingDays}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Days remaining in your cycle", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = OnSurfaceDim)
                    }
                }
                // Activity Card - Real check-in data
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("LAST CHECK-INS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = OnSurfaceDim)
                        Spacer(Modifier.height(16.dp))
                        if (uiModel.attendanceHistory.isEmpty()) {
                            Text("No visits yet", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                        } else {
                            uiModel.attendanceHistory.forEachIndexed { index, item ->
                                val color = if (item.isPrimaryColor) Color(0xFFD8BAFB) else Color(0xFF85D3DC)
                                ActivityItem(item.timeStr, item.dateStr, color)
                                if (index == 0 && uiModel.attendanceHistory.size > 1) {
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- WEEKLY ATTENDANCE SUMMARY ---
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
                        Text("WEEKLY LIMIT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = OnSurfaceDim)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${uiModel.weeklyAttendanceCount}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(" ${uiModel.weeklyAttendanceLimitText}", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceDim)
                        }
                        Spacer(Modifier.height(4.dp))
                        val isLimitReached = uiModel.isWeeklyLimitReached
                        Text(
                            if (isLimitReached) "Limit Reached" else "On track",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLimitReached) Color(0xFFD32F2F) else TealPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

                // --- RECOMMENDED ---
                Surface(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.DarkGray
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))
                        ))
                        
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                            Surface(
                                color = Color(0xFF1E2829),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("RECOMMENDED", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = TealPrimary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("HIIT: Core Burnout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Tomorrow • 7:00 AM • Studio B", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                        }
                    }
                }
                
                Spacer(Modifier.height(40.dp))
        }
    }
}
@Composable
fun ActivityItem(value: String, date: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(24.dp).background(color))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(date, style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
        }
    }
}

@Preview
@Composable
fun MemberHomeContentPreview() {
    GymTheme {
        MemberHomeContent(
            uiModel = MemberHomeUiModel(
                userName = "Jane Doe",
                planName = "Platinum",
                statusText = "Active",
                isStatusActive = true,
                renewalProgress = 0.5f,
                remainingDays = 15,
                weeklyAttendanceCount = 3,
                weeklyAttendanceLimitText = "/ 5",
                isWeeklyLimitReached = false,
                attendanceHistory = emptyList()
            ),
            onNavigate = {},
            onLogout = {}
        )
    }
}
