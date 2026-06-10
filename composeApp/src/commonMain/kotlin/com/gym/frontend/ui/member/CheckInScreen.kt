package com.gym.frontend.ui.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gym.frontend.ui.theme.*
import com.gym.frontend.ui.shared.*
import com.gym.frontend.ui.auth.TokenManager
import com.gym.shared.domain.QRToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun CheckInScreen(onDone: () -> Unit) {
    val isDark = LocalIsDarkMode.current
    val scope = rememberCoroutineScope()
    
    // Services
    val tokenManager = remember { TokenManager() }
    val accessService = remember { AccessService(tokenManager) }
    
    // State
    var qrToken by remember { mutableStateOf<QRToken?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var timeLeftSeconds by remember { mutableStateOf(0L) }
    val memberId = remember { tokenManager.getUserId() ?: "UNKNOWN" }

    // Function to refresh code
    val refreshCode = {
        isLoading = true
        errorMessage = null
        scope.launch {
            accessService.generateAccessCode()
                .onSuccess {
                    qrToken = it
                    isLoading = false
                }
                .onFailure { e ->
                    isLoading = false
                    errorMessage = e.message ?: "Could not generate access code"
                }
        }
    }

    // Initial Load
    LaunchedEffect(Unit) {
        refreshCode()
    }

    // Timer Logic
    LaunchedEffect(qrToken) {
        qrToken?.let { token ->
            while (true) {
                val now = Clock.System.now()
                val remaining = token.expiresAt.epochSeconds - now.epochSeconds
                timeLeftSeconds = remaining.coerceAtLeast(0)
                
                if (timeLeftSeconds <= 0) {
                    refreshCode() // Auto refresh when expired
                    break
                }
                delay(1000)
            }
        }
    }

    val minutes = (timeLeftSeconds / 60).toString().padStart(2, '0')
    val seconds = (timeLeftSeconds % 60).toString().padStart(2, '0')

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(
            title = "Member Access",
            subtitle = "Ready for your performance session?",
            onActionClick = { /* Settings */ }
        )

        Spacer(Modifier.height(40.dp))

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // --- MAIN ACCESS CARD ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(40.dp),
            color = if (isDark) Level1Section else Color(0xFFF0F2F2).copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Member ID Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("MEMBER ID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(memberId, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF003D44))
                    }
                    Surface(color = if (isDark) TealPrimary.copy(alpha = 0.2f) else Color(0xFF003D44), shape = RoundedCornerShape(12.dp)) {
                        Text("ACTIVE", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(32.dp))

                // QR Section — height wraps content so timer is never clipped on small screens
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isLoading && qrToken == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = TealPrimary)
                            }
                        } else {
                            // QR Frame — capped size, scales down on narrow screens
                            BoxWithConstraints(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val frameSize = minOf(maxWidth, 200.dp)
                                val innerSize = frameSize * 0.82f
                                val qrSize = frameSize * 0.73f

                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(frameSize)) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(modifier = Modifier.size(24.dp).align(Alignment.TopStart).border(width = 3.dp, color = TealPrimary, shape = RoundedCornerShape(topStart = 4.dp)))
                                        Box(modifier = Modifier.size(24.dp).align(Alignment.TopEnd).border(width = 3.dp, color = TealPrimary, shape = RoundedCornerShape(topEnd = 4.dp)))
                                        Box(modifier = Modifier.size(24.dp).align(Alignment.BottomStart).border(width = 3.dp, color = TealPrimary, shape = RoundedCornerShape(bottomStart = 4.dp)))
                                        Box(modifier = Modifier.size(24.dp).align(Alignment.BottomEnd).border(width = 3.dp, color = TealPrimary, shape = RoundedCornerShape(bottomEnd = 4.dp)))
                                    }

                                    Surface(modifier = Modifier.size(innerSize), color = Color.White, shape = RoundedCornerShape(12.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            qrToken?.let { token ->
                                                QRCodeView(
                                                    content = token.token,
                                                    modifier = Modifier.size(qrSize),
                                                    qrColor = Color.Black,
                                                    backgroundColor = Color.White
                                                )
                                            } ?: Icon(Icons.Outlined.QrCode, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(frameSize * 0.55f))

                                            if (isLoading) {
                                                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(32.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // RAW TOKEN (For testing / copy-pasting)
                            qrToken?.let { token ->
                                Text(
                                    text = token.token,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                            }

                            // Timer
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("EXPIRES IN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "$minutes:$seconds",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF003D44),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Refresh Button
                Row(
                    modifier = Modifier.clickable(enabled = !isLoading) { refreshCode() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, tint = if (isDark) TealPrimary else Color(0xFF003D44))
                    Spacer(Modifier.width(10.dp))
                    Text("REFRESH CODE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (isDark) TealPrimary else Color(0xFF003D44))
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AccessInfoCard(label = "TODAY'S GOAL", value = "Leg Power", icon = Icons.Outlined.FitnessCenter, modifier = Modifier.weight(1f))
            AccessInfoCard(label = "STUDIO", value = "North Wing", icon = Icons.Outlined.MeetingRoom, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                "Hold your device screen facing the terminal scanner. Screen brightness adjusts for optimal scanning.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun AccessInfoCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val isDark = LocalIsDarkMode.current
    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(32.dp),
        color = if (isDark) Level1Section else Color(0xFFF0F2F2).copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = if (isDark) TealPrimary else Color(0xFF003D44), modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF003D44))
        }
    }
}
