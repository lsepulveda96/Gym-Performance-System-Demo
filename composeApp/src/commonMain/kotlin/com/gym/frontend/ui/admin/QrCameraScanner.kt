package com.gym.frontend.ui.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform camera QR scanner overlay (browser on Wasm Web).
 */
@Composable
expect fun QrCameraScanner(
    modifier: Modifier,
    isActive: Boolean,
    onCodeScanned: (String) -> Unit,
    onError: (String) -> Unit
)
