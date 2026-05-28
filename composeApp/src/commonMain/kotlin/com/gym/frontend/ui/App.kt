package com.gym.frontend.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.gym.frontend.ui.theme.*
import com.gym.shared.domain.UserRole
import com.gym.frontend.ui.shared.*
import com.gym.frontend.ui.navigation.*
import com.gym.frontend.ui.auth.*
import com.gym.frontend.ui.member.*
import com.gym.frontend.ui.admin.*
import org.jetbrains.compose.resources.imageResource


@Composable
fun App() {
    var isDarkOverride by remember { mutableStateOf<Boolean?>(null) }
    val isDark = isDarkOverride ?: isSystemInDarkTheme()
    
    val tokenManager = remember { TokenManager() }
    val authRepository = remember { AuthRepository(AuthService(), tokenManager) }

    GymTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            var currentRole by remember { 
                mutableStateOf<UserRole?>(
                    if (authRepository.isSessionActive()) authRepository.getUserRole() else null
                ) 
            }
            var requestAddMemberDialog by remember { mutableStateOf(false) }
            val role = currentRole

            fun openAddMemberFlow() {
                if (role == UserRole.MEMBER) return
                requestAddMemberDialog = true
                navController.navigate("members") {
                    launchSingleTop = true
                }
            }
            
            if (role == null) {
                LoginScreen(
                    authRepository = authRepository,
                    onLogin = { newRole -> currentRole = newRole }
                )
            } else {
                MainScaffold(
                    role = role, 
                    userName = authRepository.getUserName(),
                    navController = navController,
                    tokenManager = tokenManager,
                    requestAddMemberDialog = requestAddMemberDialog,
                    onAddMemberDialogConsumed = { requestAddMemberDialog = false },
                    onAddMember = { openAddMemberFlow() },
                    onLogout = { 
                        authRepository.logout()
                        currentRole = null 
                    },
                    isDark = isDark,
                    onToggleDark = { isDarkOverride = !isDark }
                )
            }
        }
    }
}

@Composable
fun MainScaffold(
    role: UserRole, 
    userName: String?,
    navController: androidx.navigation.NavHostController,
    tokenManager: com.gym.frontend.ui.auth.TokenManager,
    requestAddMemberDialog: Boolean,
    onAddMemberDialogConsumed: () -> Unit,
    onAddMember: () -> Unit,
    onLogout: () -> Unit,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktop = maxWidth >= 900.dp && role != UserRole.MEMBER
        val maxDesktopWidth = 1440.dp

        Scaffold(
            bottomBar = {
                if (!isDesktop) {
                    MemberBottomNav(navController)
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .widthIn(max = if (isDesktop) maxDesktopWidth else Dp.Unspecified)
                ) {
                    if (isDesktop) {
                        KineticSidebar(navController, role, onAddMember = onAddMember)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (isDesktop) {
                            AdminTopBar(userName = userName, role = role, onToggleDark = onToggleDark)
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            val contentPadding = if (isDesktop) {
                                PaddingValues(horizontal = 24.dp, vertical = 20.dp)
                            } else {
                                PaddingValues(0.dp)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding)
                                    // Guarantees access to all content when browser zoom/viewport changes.
                                    .verticalScroll(rememberScrollState())
                            ) {
                                NavHost(navController = navController, startDestination = "home") {
                                    composable("home") {
                                        HomeScreen(role, userName, onNavigate = { navController.navigate(it) })
                                    }
                                    composable("members") {
                                        MembersListScreen(
                                            requestOpenAddDialog = requestAddMemberDialog,
                                            onAddDialogRequestConsumed = onAddMemberDialogConsumed
                                        )
                                    }
                                    composable("reception") { ReceptionScreen(tokenManager = tokenManager) }
                                    composable("checkin") {
                                        CheckInScreen(onDone = { navController.popBackStack() })
                                    }
                                    composable("profile") {
                                        if (role == UserRole.MEMBER) {
                                            ProfileScreen(onLogout = onLogout, isDark = isDark, onToggleDark = onToggleDark)
                                        } else {
                                            AdminSettingsScreen(onLogout = onLogout)
                                        }
                                    }
                                    composable("alerts") { AlertsScreen() }
                                }
                            }

                            if (isDesktop) {
                                ExtendedFloatingActionButton(
                                    onClick = onAddMember,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                                    containerColor = TealPrimary,
                                    contentColor = Color.Black,
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("+ Quick Action")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTopBar(userName: String?, role: UserRole, onToggleDark: () -> Unit) {
    val isDark = LocalIsDarkMode.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {

            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clip(CircleShape).clickable { }.padding(8.dp)
                )
            }


            // Help Icon
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center ) {
                Icon(
                    imageVector = Icons.Filled.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clip(CircleShape).clickable { }.padding(8.dp)
                )
            }


            // Toggle Dark - Light Mode
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center ) {
                Icon(
                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clip(CircleShape).clickable { onToggleDark() }.padding(8.dp)
                )
            }

//            Row(verticalAlignment = Alignment.CenterVertically) {
              /*  Column(horizontalAlignment = Alignment.End) {
                    Text(userName ?: "Admin", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(if (role == UserRole.RECEPTION) "RECEPTIONIST" else "OWNER", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                }*/
//                Spacer(Modifier.width(12.dp))


            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {

                // Avatar principal (círculo gris)

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { }.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp * 0.5f)
                    )
                }

                // Badge (círculo blanco con sombra)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(40.dp * 0.35f)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            clip = false
                        )
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Desplegar",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp * 0.2f)
                    )
                }
            }



//            }
        }


    }
}
