package com.gym.frontend.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gym.frontend.ui.member.AccessService
import com.gym.frontend.ui.auth.TokenManager
import com.gym.frontend.ui.shared.*
import com.gym.frontend.ui.theme.*
import com.gym.shared.domain.AccessValidationResponse
import kotlinx.coroutines.launch

@Composable
fun ReceptionScreen(tokenManager: TokenManager = TokenManager()) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(tokenManager) { ReceptionViewModel(AccessService(tokenManager)) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoading = uiState is ReceptionUiState.Validating
    val validationResult = (uiState as? ReceptionUiState.Validated)?.result

    var qrInput by remember { mutableStateOf("") }
    var isCameraActive by remember { mutableStateOf(true) }
    var cameraErrorHint by remember { mutableStateOf<String?>(null) }

    // Show snackbar on error results
    LaunchedEffect(uiState) {
        if (uiState is ReceptionUiState.Validated && validationResult?.success == false) {
            snackbarHostState.showSnackbar(validationResult.message)
        }
    }

    fun validateCode(code: String, fromCamera: Boolean = false) {
        if (code.trim().isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("No QR code detected") }
            return
        }
        viewModel.validateCode(code, fromCamera, scope)
    }

    fun clearResult() {
        qrInput = ""
        viewModel.clearResult()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(
                title = "Reception Desk",
                subtitle = "Scan member QR from camera or paste code manually"
            )

            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = if (LocalIsDarkMode.current) Level1Section else Color(0xFFF0F2F2)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CAMERA SCANNER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDim
                        )
                        FilterChip(
                            selected = isCameraActive,
                            onClick = { isCameraActive = !isCameraActive },
                            label = {
                                Text(if (isCameraActive) "Camera on" else "Camera off")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    QrCameraScanner(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .height(320.dp),
                        isActive = isCameraActive && validationResult == null && !isLoading,
                        onCodeScanned = { code ->
                            cameraErrorHint = null
                            validateCode(code, fromCamera = true)
                        },
                        onError = { message ->
                            cameraErrorHint = message
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    )

                    cameraErrorHint?.let { hint ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (isLoading) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                color = if (LocalIsDarkMode.current) Level1Section else Color(0xFFF0F2F2)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "OR PASTE CODE MANUALLY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDim
                    )
                    Spacer(Modifier.height(16.dp))

                    ModernTextField(
                        value = qrInput,
                        onValueChange = { qrInput = it },
                        placeholder = "Paste gym:access:… token",
                        icon = Icons.Outlined.QrCodeScanner,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))

                    KineticButton(
                        onClick = { validateCode(qrInput) },
                        text = "Validate Access",
                        loading = isLoading,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            validationResult?.let { result ->
                AccessValidationResultCard(
                    result = result,
                    onClear = { clearResult() }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

@Composable
fun AccessValidationResultCard(
    result: AccessValidationResponse,
    onClear: () -> Unit
) {
    val isDark = LocalIsDarkMode.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = if (result.success) {
                if (isDark) Color(0xFF1B2E2D) else Color(0xFFE8F5E9)
            } else {
                if (isDark) Color(0xFF2C1B1B) else Color(0xFFFFEBEE)
            },
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (result.success) TealPrimary else Color(0xFFFF5252)
            )
        ) {
            Row(
                modifier = Modifier.padding(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = if (result.success) TealPrimary else Color(0xFFFF5252)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (result.success) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.width(24.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (result.success) "ACCESS GRANTED" else "ACCESS DENIED",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (result.success) {
                            if (isDark) Color.White else Color(0xFF1B5E20)
                        } else {
                            if (isDark) Color.White else Color(0xFFB71C1C)
                        }
                    )

                    result.memberName?.let { name ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDark) OnSurfaceDim else {
                            if (result.success) Color(0xFF2E7D32) else Color(0xFFC62828)
                        }
                    )

                    if (result.memberName != null && result.success) {
                        Spacer(Modifier.height(24.dp))

                        val daysRemaining = result.expirationDate?.let {
                            val now = kotlinx.datetime.Clock.System.now()
                            (it.toEpochMilliseconds() - now.toEpochMilliseconds()) / (1000 * 60 * 60 * 24)
                        } ?: -1
                        val isExpiringSoon = daysRemaining in 0..3

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailRow("PLAN", result.planName ?: "N/A")
                            DetailRow(
                                "WEEKLY VISITS",
                                "${result.currentWeeklyAccessCount ?: 0} of ${result.weeklyAccessLimit ?: "∞"}"
                            )
                            DetailRow(
                                "EXPIRATION",
                                result.expirationDate?.toString()?.take(10) ?: "N/A",
                                textColor = if (isExpiringSoon) Color(0xFFFF5252) else null
                            )
                        }

                        if (isExpiringSoon) {
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                color = Color(0xFFFF5252).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "RENEWAL REQUIRED SOON",
                                        color = Color(0xFFFF5252),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
            Text("Clear and scan next", color = OnSurfaceDim)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, textColor: Color? = null) {
    val isDark = LocalIsDarkMode.current
    Surface(
        color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = OnSurfaceDim)
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = textColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
