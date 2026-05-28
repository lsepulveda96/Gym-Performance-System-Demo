package com.gym.frontend.ui.member

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gym.frontend.ui.theme.*
import com.gym.frontend.ui.shared.*

@Composable
fun AlertsScreen() {
    val notificationList = remember { 
        mutableStateListOf(
            Triple("Class starts in 30m", "Your 'HIIT: Core Burnout' session is about to start.", "30m ago"),
            Triple("Payment Successful", "Monthly membership for October has been processed.", "2h ago"),
            Triple("New Achievement!", "You've hit a 10-day workout streak. Keep up the kinetic energy!", "Yesterday"),
            Triple("Coach Message", "Sarah left a comment on your last performance.", "Yesterday")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = "Notifications",
            actionLabel = if (notificationList.isNotEmpty()) "Clear all" else null,
            onActionClick = { notificationList.clear() }
        )

        if (notificationList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.DoneAll, contentDescription = "Done all")
                    Spacer(Modifier.height(16.dp))
                    Text("You're all caught up!", style = MaterialTheme.typography.titleMedium, color = OnSurfaceDim)
                }
            }
        } else {
            // --- FILTER CHIPS ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KineticFilterChip("All", true)
                KineticFilterChip("Bookings", false)
                KineticFilterChip("Account", false)
            }

            notificationList.forEach { (title, desc, time) ->
                AlertCard(
                    title = title,
                    description = desc,
                    time = time,
                    icon = Icons.Outlined.EventAvailable,
                    iconBg = Color(0xFFE8F5E9).copy(alpha = 0.1f)
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}
