package com.gym.frontend.ui.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.gym.frontend.ui.theme.*
import org.jetbrains.compose.resources.DrawableResource
import gym_system.composeapp.generated.resources.*

fun getAvatarResource(name: String?): DrawableResource? {
    return when(name) {
        "avatar_cardio" -> Res.drawable.avatar_cardio
        "avatar_fuel" -> Res.drawable.avatar_fuel
        "avatar_paz" -> Res.drawable.avatar_paz
        "avatar_pose" -> Res.drawable.avatar_pose
        "avatar_running" -> Res.drawable.avatar_running
        "avatar_strength" -> Res.drawable.avatar_strength
        "avatar_triumph" -> Res.drawable.avatar_triumph
        "avatar_weight" -> Res.drawable.avatar_weight
        else -> null
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceNeutral
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceDim
                )
            }
        }
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp).clickable { onActionClick?.invoke() }
            )
        }
    }
}

@Composable
fun KineticButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF003D44), 
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor, 
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled && !loading
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            if (!loading && (text.contains("Dashboard") || text.contains("Access") || text.contains("Sign In"))) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    readOnly: Boolean = false,
    errorText: String? = null,
    icon: ImageVector? = null
) {
    val isDark = LocalIsDarkMode.current
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(placeholder) },
        placeholder = { Text(placeholder, color = OnSurfaceDim.copy(alpha = 0.5f)) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) } },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        readOnly = readOnly,
        singleLine = true,
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TealPrimary,
            unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.LightGray,
            focusedLabelColor = TealPrimary,
            unfocusedLabelColor = OnSurfaceDim,
            focusedContainerColor = if (isDark) Level1Section else Color(0xFFF8F9F9),
            unfocusedContainerColor = if (isDark) Level1Section else Color(0xFFF8F9F9),
            focusedTextColor = if (isDark) Color.White else Color.Black,
            unfocusedTextColor = if (isDark) Color.White else Color.Black
        )
    )
}

@Composable
fun StatusMiniCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val isDark = LocalIsDarkMode.current
    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) Level1Section else Color(0xFFF5F7F7)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AlertCard(title: String, description: String, time: String, icon: ImageVector, iconBg: Color = Color.Transparent) {
    val isDark = LocalIsDarkMode.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Level1Section else Color.White,
        border = if (!isDark) BorderStroke(1.dp, Color(0xFFEEEEEE)) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (iconBg == Color.Transparent) TealPrimary.copy(alpha = 0.1f) else iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (iconBg == Color.Transparent) TealPrimary else Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(time, style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
                }
                Text(description, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceDim)
            }
        }
    }
}

@Composable
fun KineticFilterChip(label: String, selected: Boolean) {
    Surface(
        modifier = Modifier.height(36.dp).clickable { },
        color = if (selected) TealPrimary else (if (LocalIsDarkMode.current) Level1Section else Color(0xFFF0F2F2)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.Black else OnSurfaceNeutral)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceDim)
    }
}

@Composable
fun TableHeader(
    text: String, 
    modifier: Modifier = Modifier, 
    sortable: Boolean = false, 
    isSorted: Boolean = false, 
    ascending: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.then(if (sortable) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = FontWeight.Bold, 
            color = if (isSorted) MaterialTheme.colorScheme.primary else OnSurfaceDim
        )
        if (sortable) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (isSorted && !ascending) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                contentDescription = null,
                modifier = Modifier.size(14.dp).padding(top = 1.dp),
                tint = if (isSorted) MaterialTheme.colorScheme.primary else OnSurfaceDim.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun SocialButton(icon: ImageVector, label: String) {
    val isDark = LocalIsDarkMode.current
    Surface(
        modifier = Modifier.width(160.dp).height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Level1Section else Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PaginationDot(text: String, active: Boolean) {
    Surface(
        color = if (active) {
            if (LocalIsDarkMode.current) TealPrimary else Color(0xFF003D44)
        } else Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = if (active) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QRCodeView(
    content: String,
    modifier: Modifier = Modifier,
    qrColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val qrMatrix = androidx.compose.runtime.remember(content) {
        SimpleQRGenerator.generate(content)
    }

    Canvas(modifier = modifier) {
        val gridSize = qrMatrix.size
        if (gridSize == 0) return@Canvas
        
        val cellSize = size.width / gridSize

        // Background
        drawRect(color = backgroundColor)

        // QR Code Pixels
        for (row in 0 until gridSize) {
            val rowData = qrMatrix[row]
            for (col in 0 until gridSize) {
                if (rowData[col]) {
                    drawRect(
                        color = qrColor,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x = col * cellSize,
                            y = row * cellSize
                        ),
                        size = androidx.compose.ui.geometry.Size(cellSize + 1f, cellSize + 1f)
                    )
                }
            }
        }
    }
}
