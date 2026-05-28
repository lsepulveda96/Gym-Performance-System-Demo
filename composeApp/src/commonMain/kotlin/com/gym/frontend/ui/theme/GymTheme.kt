package com.gym.frontend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.Black,
    primaryContainer = TealContainer,
    onPrimaryContainer = Color.White,
    secondary = SlateSecondary,
    onSecondary = OnSurfaceNeutral,
    tertiary = LavenderTertiary,
    onTertiary = Color.Black,
    background = Level0Base,
    onBackground = OnSurfaceNeutral,
    surface = Level1Section,
    onSurface = OnSurfaceNeutral,
    surfaceVariant = Level2Card,
    onSurfaceVariant = OnSurfaceDim,
    outlineVariant = OutlineVariant,
    surfaceContainer = Level1Section, // M3.1.2+
    surfaceContainerLow = Level1Section,
    surfaceContainerHigh = Level2Card,
    surfaceContainerHighest = Level3Highest
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF005B63), // Deep Teal from the image
    onPrimary = Color.White,
    secondary = Color(0xFFE9EEF0), // Soft Gray for non-primary cards
    tertiary = Color(0xFF8B5CF6),
    background = Color(0xFFF7F8F7), // Off-white background
    surface = Color.LightGray,
    onSurface = Color(0xFF101415),
    surfaceVariant = Color(0xFFF0F2F2),
    outlineVariant = Color(0x1F000000)
)

val LocalIsDarkMode = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
fun GymTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    androidx.compose.runtime.CompositionLocalProvider(LocalIsDarkMode provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GymTypography,
            content = content
        )
    }
}
