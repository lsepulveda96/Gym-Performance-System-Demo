package com.gym.frontend.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gym.frontend.ui.theme.*
import com.gym.frontend.ui.shared.*

import com.gym.shared.domain.DashboardSummary
import androidx.compose.runtime.*

import org.koin.compose.koinInject

@Composable
fun AdminDashboardContent() {
    val isDark = LocalIsDarkMode.current
    val scope = rememberCoroutineScope()
    val viewModel = koinInject<AdminDashboardViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData(scope)
    }

    val uiModel: AdminDashboardUiModel? = (uiState as? AdminDashboardUiState.Success)?.uiModel
    val isLoading = uiState is AdminDashboardUiState.Loading

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = TealPrimary, trackColor = TealPrimary.copy(alpha = 0.1f))
        }
        if (uiState is AdminDashboardUiState.Error) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        (uiState as AdminDashboardUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.loadData(scope) }) { Text("Retry") }
                }
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            // --- HEADER ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("System Overview", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Real-time performance metrics for The Kinetic Editorial", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant, 
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Last 7 Days", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- STATS GRID ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AdminStatCard("Active Members", uiModel?.activeMembers ?: "0", "Live", Modifier.weight(1f))
            AdminStatCard("Expired Members", uiModel?.expiredMembers ?: "0", "Alert", Modifier.weight(1f), isTrendNegative = true)
            AdminStatCard("Expiring < 7d", uiModel?.expiringSoon ?: "0", "Risk", Modifier.weight(1f), isTrendNegative = true)
            AdminStatCard("Today Check-ins", uiModel?.todayCheckIns ?: "0", "Daily", Modifier.weight(1f))

        }

        Spacer(Modifier.height(32.dp))

        // --- MIDDLE SECTION: REVENUE FLOW & OVERDUE RISK ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // Revenue Flow Chart
            Surface(
                modifier = Modifier.weight(2f).height(400.dp), 
                shape = RoundedCornerShape(32.dp), 
                color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(32.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Revenue Flow", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Net earnings vs. operating costs", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendItem("Revenue", TealPrimary)
                            LegendItem("Costs", LavenderTertiary)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    // Revenue Flow Chart
                    Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        val revenueData = uiModel?.revenueFlow ?: emptyList()
                        val maxRevenue = revenueData.map { it.amount }.maxOrNull() ?: 1.0
                        
                        revenueData.forEachIndexed { index, data ->
                            val heightFactor = if (maxRevenue > 0) (data.amount / maxRevenue).toFloat().coerceIn(0.1f, 1f) else 0.1f
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .fillMaxHeight(heightFactor)
                                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                        .background(if (index == revenueData.size - 1) TealPrimary else TealPrimary.copy(alpha = 0.2f))
                                )
                                val textColor = if (index == revenueData.size - 1) {
                                    if (isDark) OnSurfaceNeutral else MaterialTheme.colorScheme.onSurface 
                                } else {
                                    if (isDark) OnSurfaceDim else MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(data.month, style = MaterialTheme.typography.labelSmall, color = textColor)
                            }
                        }
                    }
                }
            }

            // Overdue Risk
            Surface(
                modifier = Modifier.weight(1f).height(400.dp), 
                shape = RoundedCornerShape(32.dp), 
                color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Overdue Risk", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Surface(color = Color(0xFF421212), shape = RoundedCornerShape(8.dp)) {
                            Text("URGENT", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFFFF5252), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    
                    val riskMembers = uiModel?.overdueRisk ?: emptyList()
                    if (riskMembers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No immediate risks", color = OnSurfaceDim, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        riskMembers.forEach { risk ->
                            RiskItem(risk.name, risk.details, risk.amountStr)
                        }
                    }
                    
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("View All Arrears", color = OnSurfaceNeutral)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- RECENT ARRIVALS ---
        Surface(
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(32.dp), 
            color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Recent Arrivals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Live floor activity", color = if (isDark) OnSurfaceDim else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFF102A2B), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(uiModel?.todayCheckIns ?: "0", color = TealPrimary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("TODAY'S TOTAL", style = MaterialTheme.typography.labelSmall, color = if (isDark) OnSurfaceDim else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val arrivals = uiModel?.recentArrivals ?: emptyList()
                    if (arrivals.isEmpty()) {
                        Text("No recent arrivals", color = OnSurfaceDim, modifier = Modifier.padding(16.dp))
                    } else {
                        arrivals.forEach { arrival ->
                            ArrivalItem(arrival.name, arrival.plan, arrival.relativeTime, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(100.dp))
    }
}
}

@Composable
fun AdminStatCard(title: String, value: String, trend: String, modifier: Modifier = Modifier, isTrendNegative: Boolean = false) {
    val isDark = LocalIsDarkMode.current
    Surface(
        modifier = modifier.height(140.dp), 
        shape = RoundedCornerShape(24.dp), 
        color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = if (isDark) OnSurfaceDim else MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = if (isDark) OnSurfaceDim else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = if (isDark) OnSurfaceNeutral else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    val isPos = !isTrendNegative
                    Icon(
                        imageVector = if (isPos) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isPos) TealPrimary else Color(0xFFFF5252)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(trend, color = if (isTrendNegative) Color(0xFFFF5252) else TealPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(thickness = 4.dp, color = TealPrimary.copy(alpha = 0.3f), modifier = Modifier.width(80.dp).clip(CircleShape))
        }
    }
}

@Composable
fun RiskItem(name: String, details: String, amount: String) {
    val isDark = LocalIsDarkMode.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.DarkGray.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = if (isDark) OnSurfaceNeutral else MaterialTheme.colorScheme.onSurface)
            Text(details, color = if (isDark) OnSurfaceDim else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(amount, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text("REMIND", color = TealPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ArrivalItem(name: String, activity: String, time: String, modifier: Modifier = Modifier) {
    val isDark = LocalIsDarkMode.current
    Surface(
        modifier = modifier, 
        shape = RoundedCornerShape(20.dp), 
        color = if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TealPrimary).align(Alignment.BottomEnd))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = if (isDark) OnSurfaceNeutral else MaterialTheme.colorScheme.onSurface)
                Text(activity, color = if (isDark) OnSurfaceDim else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            Text(time, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (time == "JUST NOW") TealPrimary else OnSurfaceDim)
        }
    }
}
