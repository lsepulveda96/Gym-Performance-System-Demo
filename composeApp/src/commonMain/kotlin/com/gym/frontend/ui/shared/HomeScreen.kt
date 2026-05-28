package com.gym.frontend.ui.shared

import androidx.compose.runtime.Composable
import com.gym.shared.domain.UserRole
import com.gym.frontend.ui.member.MemberHomeContent
import com.gym.frontend.ui.admin.AdminDashboardContent

@Composable
fun HomeScreen(role: UserRole, userName: String?, onNavigate: (String) -> Unit) {
    if (role == UserRole.MEMBER) {
        MemberHomeContent(onNavigate)
    } else {
        AdminDashboardContent()
    }
}
