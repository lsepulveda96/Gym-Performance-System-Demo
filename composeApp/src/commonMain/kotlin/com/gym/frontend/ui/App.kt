package com.gym.frontend.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
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


import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import com.gym.frontend.di.frontendModule

@Composable
fun App() {
    KoinApplication(application = {
        modules(frontendModule)
    }) {
        var isDarkOverride by remember { mutableStateOf<Boolean?>(null) }
        val isDark = isDarkOverride ?: isSystemInDarkTheme()
        
        val tokenManager = koinInject<TokenManager>()
        val authRepository = koinInject<AuthRepository>()

    GymTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var currentRole by remember {
                mutableStateOf<UserRole?>(
                    if (authRepository.isSessionActive()) authRepository.getUserRole() else null
                )
            }
            var requestAddMemberDialog by remember { mutableStateOf(false) }
            val role = currentRole

            if (role == null) {
                LoginScreen(
                    onLogin = { newRole -> currentRole = newRole }
                )
            } else {
                // Fresh NavController per logged-in session avoids stale routes after logout/relogin.
                val navController = rememberNavController()

                fun openAddMemberFlow() {
                    if (role == UserRole.MEMBER) return
                    requestAddMemberDialog = true
                    navController.navigate("members") {
                        launchSingleTop = true
                    }
                }

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
                            AdminTopBar(
                                userName = userName,
                                role = role,
                                onToggleDark = onToggleDark,
                                onLogout = onLogout
                            )
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            val contentPadding = if (isDesktop) {
                                PaddingValues(horizontal = 24.dp, vertical = 20.dp)
                            } else {
                                PaddingValues(0.dp)
                            }

                            // NavHost must NOT be inside verticalScroll — unbounded height = blank screens.
                            // Each destination handles its own scrolling internally.
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding)
                            ) {
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

                            /*if (isDesktop) {
                                ExtendedFloatingActionButton(
                                    onClick = onAddMember,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                                    containerColor = TealPrimary,
                                    contentColor = Color.Black,
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("+ Quick Action")
                                }
                            }*/
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTopBar(
    userName: String?,
    role: UserRole,
    onToggleDark: () -> Unit,
    onLogout: () -> Unit
) {
    val isDark = LocalIsDarkMode.current
    var showMenu by remember { mutableStateOf(false) }

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

            Box {
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
                            .clickable { showMenu = true }.padding(8.dp),
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

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    offset = DpOffset(0.dp, 8.dp),
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Profile", fontWeight = FontWeight.Medium) },
                        onClick = { showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                    DropdownMenuItem(
                        text = { Text("Log out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                        onClick = {
                            showMenu = false
                            onLogout()
                        },
                        leadingIcon = { Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                    )
                }
            }
        }
    }
}
