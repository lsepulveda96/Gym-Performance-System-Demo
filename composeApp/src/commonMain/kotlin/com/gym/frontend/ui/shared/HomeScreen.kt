package com.gym.frontend.ui.shared

import androidx.compose.runtime.Composable
import com.gym.shared.domain.UserRole
import com.gym.frontend.ui.member.MemberHomeScreen
import com.gym.frontend.ui.admin.AdminDashboardScreen

@Composable
fun HomeScreen(role: UserRole, userName: String?, onNavigate: (String) -> Unit) {
    if (role == UserRole.MEMBER) {
        MemberHomeScreen(onNavigate)
    } else {
        AdminDashboardScreen()
    }
}
