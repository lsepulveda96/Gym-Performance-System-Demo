package com.gym.frontend.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import com.gym.frontend.ui.auth.LoginUiState
import com.gym.frontend.ui.theme.*
import com.gym.shared.domain.*
import com.gym.frontend.ui.shared.*
import gym_system.composeapp.generated.resources.Res

import gym_system.composeapp.generated.resources.img_gym_login

import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import com.gym.frontend.ui.config.AppConfig
import org.jetbrains.compose.ui.tooling.preview.Preview



data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val selectedRole: UserRole = UserRole.MEMBER,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLogin: (UserRole) -> Unit
) {
    var state by remember { mutableStateOf(LoginUiState()) }
    val scope = rememberCoroutineScope()

    LoginContent(
        state = state,
        onEmailChange = { state = state.copy(email = it, errorMessage = null) },
        onPasswordChange = { state = state.copy(password = it, errorMessage = null) },
        //onRoleChange = { state = state.copy(selectedRole = it, errorMessage = null) },
        onRoleChange = { role -> state = state.copy(selectedRole = role, email = "", password = "", errorMessage = null) },
        onLoginClick = {
            state = state.copy(isLoading = true, errorMessage = null)
            scope.launch {
                val result = authRepository.login(LoginRequest(state.email, state.password, state.selectedRole))
                result.onSuccess { 
                    state = state.copy(isLoading = false)
                    onLogin(it.user.role) 
                }.onFailure { 
                    state = state.copy(
                        isLoading = false,
                        errorMessage = if (state.selectedRole == UserRole.MEMBER) "Invalid email or DNI" else "Invalid credentials"
                    )
                }
            }
        },
        onFillAdminDemo = {
            state = state.copy(
                selectedRole = UserRole.OWNER,
                email = "admin@demo.com",
                password = "1234",
                errorMessage = null
            )
        },
        onFillMemberDemo = {
            state = state.copy(
                selectedRole = UserRole.MEMBER,
                email = "member@demo.com",
                password = "1234",
                errorMessage = null
            )
        }
    )
}

@Composable
fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onLoginClick: () -> Unit,
    onFillAdminDemo: () -> Unit,
    onFillMemberDemo: () -> Unit
) {
    val isDark = LocalIsDarkMode.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(if (isDark) Color.Black else Color(0xFFF0F2F2)),
        contentAlignment = Alignment.Center
    ) {
        val isDesktop = maxWidth > 900.dp
        
        if (isDesktop && state.selectedRole == UserRole.OWNER) {
            // web admin login
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(16.dp)
                    .widthIn(max = 1400.dp),
                shape = RoundedCornerShape(48.dp),
                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                shadowElevation = 8.dp
            ) {
                AtelierAdminLogin(
                    state = state,
                    email = state.email,
                    onEmailChange = onEmailChange,
                    password = state.password,
                    onPasswordChange = onPasswordChange,
                    errorMessage = state.errorMessage,
                    isLoading = state.isLoading,
                    onLogin = onLoginClick,
                    onSwitchToMember = { onRoleChange(UserRole.MEMBER) },
                    onFillAdminDemo = onFillAdminDemo,
                    onRoleChange = onRoleChange
                )
            }
        } else {
            // Mobile Login
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Branding
                Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF003D44)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("KINETIC", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Text("PERFORMANCE ECOSYSTEM", style = MaterialTheme.typography.labelSmall, color = Color(0xFF003D44), letterSpacing = 2.sp)

                Spacer(Modifier.height(48.dp))
                RoleSelector(state.selectedRole, onRoleChange)
                Spacer(Modifier.height(40.dp))

                Column(modifier = Modifier.widthIn(max = 400.dp)) {
                    Text(if (state.selectedRole == UserRole.OWNER) "Admin Login" else "Welcome Back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Access your dashboard to track performance.", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
                    Spacer(Modifier.height(32.dp))

                    if (AppConfig.demoMode) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("DEMO MODE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = TealPrimary)
                                Text("Use demo credentials or edit below.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    //OutlinedButton(onClick = onFillAdminDemo) { Text("Admin demo") }
                                    OutlinedButton(onClick = onFillMemberDemo) { Text("Member demo") }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    ModernTextField(value = state.email, onValueChange = onEmailChange, placeholder = "Email Address", icon = Icons.Outlined.Person, errorText = if (state.errorMessage != null) "" else null)
                    Spacer(Modifier.height(16.dp))
                    ModernTextField(
                        value = state.password, 
                        onValueChange = onPasswordChange, 
                        placeholder = if (state.selectedRole == UserRole.MEMBER) "DNI" else "Password", 
                        isPassword = state.selectedRole != UserRole.MEMBER, 
                        icon = if (state.selectedRole == UserRole.MEMBER) Icons.Outlined.Badge else Icons.Outlined.Lock, 
                        errorText = state.errorMessage
                    )

                    Spacer(Modifier.height(32.dp))
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF003D44))
                    } else {
                        KineticButton(onClick = onLoginClick, text = "Sign In", modifier = Modifier.fillMaxWidth().height(56.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun AtelierAdminLogin(
    state: LoginUiState,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onLogin: () -> Unit,
    onSwitchToMember: () -> Unit,
    onFillAdminDemo: () -> Unit,
    onRoleChange: (UserRole) -> Unit,
) {
    val isDark = LocalIsDarkMode.current
    val atelierTeal = Color(0xFF003D44)

    Row(modifier = Modifier.fillMaxSize()) {
        // --- VISUAL SIDE ---
        Box(
            modifier = Modifier.weight(1.2f).fillMaxHeight().background(if (isDark) Color(0xFF1A1A1A) else Color(0xFFF0F2F2))
        ) {
            Image(

                painter = painterResource(Res.drawable.img_gym_login),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            Column(modifier = Modifier.fillMaxSize().padding(60.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = atelierTeal, shape = CircleShape, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.padding(6.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Kinetic Atelier", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }

                Column {
                    Text("Elevating", style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, color = OnSurfaceNeutral), fontWeight = FontWeight.ExtraBold)
                    Text("Athletic", style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, color = TealPrimary), fontWeight = FontWeight.ExtraBold)
                    Text("Management.", style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, color = OnSurfaceNeutral), fontWeight = FontWeight.ExtraBold)
                    
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "A curated digital workspace for the modern high-end gym experience. Orchestrate movement with editorial precision.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.width(420.dp),
                        color = OnSurfaceDim
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    Column {
                        Text("12.4k", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("MEMBERS MANAGED", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim, letterSpacing = 1.sp)
                    }
                    Column {
                        Text("99.9%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("SYSTEM UPTIME", style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim, letterSpacing = 1.sp)
                    }
                }
            }
        }

        // --- FORM SIDE ---
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().background(if (isDark) Level1Section else Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Welcome Back", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Enter your credentials to access the Atelier Admin panel.", color = OnSurfaceDim, style = MaterialTheme.typography.bodyMedium)
                
                Spacer(Modifier.height(40.dp))
                RoleSelector(state.selectedRole, onRoleChange)
                Spacer(Modifier.height(40.dp))

                if (AppConfig.demoMode) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "DEMO MODE",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = TealPrimary
                            )
                            Text(
                                "Use admin demo credentials or edit below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceDim
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = onFillAdminDemo) { Text("Admin demo ") }
                                //OutlinedButton(onClick = onSwitchToMember) { Text("Member login") }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text("ADMIN EMAIL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                ModernTextField(value = email, onValueChange = onEmailChange, placeholder = "Email address", icon = Icons.Outlined.Email, errorText = if (errorMessage != null) "" else null)

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("PASSWORD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Forgot Password?", color = OnSurfaceDim, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {})
                }
                Spacer(Modifier.height(8.dp))
                ModernTextField(value = password, onValueChange = onPasswordChange, placeholder = "Password", icon = Icons.Outlined.Lock, isPassword = true, errorText = errorMessage)

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(20.dp).border(1.dp, Color.LightGray, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text("Keep me logged in for 30 days", style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
                }

                Spacer(Modifier.height(40.dp))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = atelierTeal)
                } else {
                    KineticButton(onClick = onLogin, text = "Access Dashboard", modifier = Modifier.fillMaxWidth().height(64.dp))
                }

                Spacer(Modifier.height(40.dp))
                HorizontalDivider(color = Color.LightGray.copy(0.3f), thickness = 1.dp)
                Spacer(Modifier.height(24.dp))
                Text("Secured by Atelier Protocol. Learn more", modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
            }
        }
    }
}

@Composable
fun RoleSelector(selectedRole: UserRole, onRoleSelect: (UserRole) -> Unit) {
    val isDark = LocalIsDarkMode.current
    Surface(
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) Level1Section else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            val roles = listOf(UserRole.MEMBER, UserRole.OWNER)
            roles.forEach { role ->
                val active = selectedRole == role
                Surface(
                    modifier = Modifier.weight(1f).height(40.dp).clickable { onRoleSelect(role) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) (if (isDark) Color.White else Color(0xFF003D44)) else Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (role == UserRole.OWNER) "Admin" else "Member",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (active) (if (isDark) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview()
@Composable
fun LoginContentPreview() {
    GymTheme{
        LoginContent(
            state = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onRoleChange = {},
            onLoginClick = {},
            onFillAdminDemo = {},
            onFillMemberDemo = {}
        )
    }
}
