package com.gym.frontend.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A Kinertic Editorial Glass Card.
 * Uses backdrop blur and semi-transparent surface tokens.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            // Backdrop blur is harder to achieve in pure Compose without specific targets,
            // but we can simulate the glass effect with alpha and gradients.
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

/**
 * Premium Button with the signature Teal Gradient.
 */
@Composable
fun KineticButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues()
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = TealPrimary,
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        )
        {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.Black
            )
        }
    }
}

/**
 * A layout section that follows the "No-Line" rule using tonal transitions.
 */
@Composable
fun TonalSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        content()
    }
}
