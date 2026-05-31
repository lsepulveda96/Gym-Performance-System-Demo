package com.gym.frontend.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gym.frontend.ui.theme.*
import com.gym.frontend.ui.shared.*

@Composable
fun MemberBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDark = LocalIsDarkMode.current

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp), // Ergonomic height for bottom navigation
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(bottom = 8.dp), // Room for system bar
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                NavigationItemData("home", Icons.Outlined.Home),
                NavigationItemData("checkin", Icons.Outlined.QrCode),
                NavigationItemData("alerts", Icons.Outlined.Notifications),
                NavigationItemData("profile", Icons.Outlined.Person)
            )

            items.forEach { item ->
                BottomNavItemCustom(
                    icon = item.icon,
                    selected = currentRoute == item.route,
                    isDark = isDark,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                )
            }
        }
    }
}

data class NavigationItemData(val route: String, val icon: ImageVector)

@Composable
fun BottomNavItemCustom(
    icon: ImageVector,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // No ripple for a cleaner look as requested by design
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected && isDark) {
            // Rounded Square Background for Selected Mode
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(20.dp),
                color = NavSelectedBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NavSelectedIcon,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        } else {
            // Unselected or Light Mode
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) NavSelectedIcon else NavUnselected,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(if (selected) 1f else 0.7f)
            )
        }
    }
}

@Composable
fun KineticSidebar(
    navController: NavHostController,
    role: com.gym.shared.domain.UserRole,
    onAddMember: () -> Unit = {}
) {
    val isDark = LocalIsDarkMode.current
    Surface(
        modifier = Modifier.width(280.dp).fillMaxHeight(),
        color = if (isDark) Color(0xFF03080A) else Color.White,
        border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Logo
            Column(modifier = Modifier.padding(start = 8.dp, top = 24.dp)) {
                Text(
                    "KINETIC",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isDark) TealPrimary else Color.Black,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "PREMIUM MANAGEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) OnSurfaceDim else Color.Gray,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(Modifier.height(48.dp))
            
            // Navigation Items
            val items = if (role == com.gym.shared.domain.UserRole.MEMBER) {
                listOf(
                    Triple("Home", Icons.Outlined.Home, "home"),
                    Triple("My Access", Icons.Outlined.QrCodeScanner, "checkin"),
                    Triple("Profile", Icons.Outlined.Person, "profile")
                )
            } else {
                listOf(
                    Triple("Dashboard", Icons.Outlined.Home, "home"),
                    Triple("Reception", Icons.Outlined.QrCodeScanner, "reception"),
                    Triple("Members", Icons.Outlined.Groups, "members"),
                    Triple("Settings", Icons.Outlined.Settings, "profile")
                )
            }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            items.forEach { (label, icon, route) ->
                val selected = currentRoute == route
                SidebarItem(label, icon, selected, isDark) {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }
            }
            
         /*   Spacer(Modifier.weight(1f))

            KineticButton(
                onClick = onAddMember,
                text = "+ Add New Member",
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )

            Spacer(Modifier.height(24.dp))*/
        }
    }
}

@Composable
fun SidebarItem(label: String, icon: ImageVector, selected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        color = if (selected) {
            if (isDark) Color(0xFF102A2B) else Color(0xFFF0F2F2).copy(alpha = 0.5f)
        } else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) NavSelectedIcon else NavUnselected,
                modifier = Modifier
                    .size(24.dp)
                    .alpha(if (selected) 1f else 0.7f)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    if (isDark) TealPrimary else Color(0xFF005B63)
                } else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            if (selected) {
                Box(modifier = Modifier.width(3.dp).height(24.dp).background(TealPrimary).clip(CircleShape))
            }
        }
    }
}
