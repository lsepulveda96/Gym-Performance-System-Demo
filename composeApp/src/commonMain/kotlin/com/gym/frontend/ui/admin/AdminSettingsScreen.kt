package com.gym.frontend.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gym.frontend.ui.theme.*

@Composable
fun AdminSettingsScreen(onLogout: () -> Unit) {
    val isDark = LocalIsDarkMode.current
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp).verticalScroll(rememberScrollState())) {
        // --- HEADER ---
        Spacer(Modifier.height(24.dp))
        Text("Gym Settings", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Text("Configure your kinetic environment and business parameters", color = OnSurfaceDim, style = MaterialTheme.typography.bodyLarge)
        
        Spacer(Modifier.height(48.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            // General Settings Column
            Column(modifier = Modifier.weight(1f)) {
                SettingsSectionTitle("GENERAL CONFIGURATION")
                SettingsCard(title = "Gym Profile", description = "Name, description and public location", icon = Icons.Outlined.Home)
                SettingsCard(title = "Business Identity", description = "Tax ID, Billing address and Currency", icon = Icons.Outlined.Info)
                SettingsCard(title = "Operational Hours", description = "Opening times and peak management", icon = Icons.Outlined.Settings)
            }
            
            // System & Team Column
            Column(modifier = Modifier.weight(1f)) {
                SettingsSectionTitle("SYSTEM & TEAM")
                var showPlansManagement by remember { mutableStateOf(false) }
                
                SettingsCard(
                    title = "Membership Plans", 
                    description = "Manage subscription plans, prices and durations", 
                    icon = Icons.Outlined.Assignment,
                    onClick = { showPlansManagement = true }
                )
                
                if (showPlansManagement) {
                    PlansManagementDialog(onDismiss = { showPlansManagement = false })
                }
                
                SettingsCard(title = "Team Management", description = "Staff roles, permissions and roster", icon = Icons.Outlined.Groups)
                SettingsCard(title = "Access Control", description = "QR entry points and gate hardware logic", icon = Icons.Outlined.QrCode)
            }
        }
        
        Spacer(Modifier.height(64.dp))
        
        // Danger Zone
        SettingsSectionTitle("ACCOUNT MANAGEMENT")
        Button(
            onClick = onLogout,
            modifier = Modifier.width(280.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF2C1B1B) else Color(0xFFFFF3F1),
                contentColor = Color(0xFFD32F2F)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Log Out Admin Dashboard", fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = TealPrimary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 24.dp),
        letterSpacing = 1.2.sp
    )
}

@Composable
fun SettingsCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) {
    val isDark = LocalIsDarkMode.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant,
        border = if (!isDark) BorderStroke(1.dp, Color(0xFFEEEEEE)) else null
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isDark) OnSurfaceNeutral else Color.Black)
                Text(description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
            }
            Text("→", color = OnSurfaceDim, style = MaterialTheme.typography.titleLarge)
        }
    }
}
