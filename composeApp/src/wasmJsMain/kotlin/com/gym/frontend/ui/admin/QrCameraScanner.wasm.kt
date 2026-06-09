package com.gym.frontend.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gym.frontend.ui.theme.OnSurfaceDim
import com.gym.frontend.ui.theme.TealPrimary
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import kotlin.js.JsName

@JsName("kineticQrStart")
private external fun kineticQrStart(elementId: String)

@JsName("kineticQrStop")
private external fun kineticQrStop()

@JsName("kineticQrReadDetail")
private external fun kineticQrReadDetail(event: Event): String

// Helpers para convertir coordenadas de Compose → CSS.
// El canvas de Compose puede no estar en (0,0) y puede tener escala diferente a devicePixelRatio.
@JsName("kineticCanvasCssLeft")
private external fun kineticCanvasCssLeft(): Double

@JsName("kineticCanvasCssTop")
private external fun kineticCanvasCssTop(): Double

@JsName("kineticCanvasCssWidth")
private external fun kineticCanvasCssWidth(): Double

@JsName("kineticCanvasCssHeight")
private external fun kineticCanvasCssHeight(): Double

@JsName("kineticCanvasPhysWidth")
private external fun kineticCanvasPhysWidth(): Double

@JsName("kineticCanvasPhysHeight")
private external fun kineticCanvasPhysHeight(): Double

private enum class ScannerPhase {
    Idle,
    Starting,
    Scanning,
    Error
}

@Composable
actual fun QrCameraScanner(
    modifier: Modifier,
    isActive: Boolean,
    onCodeScanned: (String) -> Unit,
    onError: (String) -> Unit
) {
    val density = LocalDensity.current.density
    val elementId = remember { "kinetic-qr-mount-${(0..999_999).random()}" }
    var windowBounds by remember { mutableStateOf<WindowBounds?>(null) }
    var phase by remember { mutableStateOf(ScannerPhase.Idle) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userRequestedCamera by remember { mutableStateOf(false) }
    var startNonce by remember { mutableStateOf(0) }

    val shouldRunCamera = isActive && userRequestedCamera

    fun stopCameraHardware() {
        kineticQrStop()
        removeOverlay(elementId)
    }

    fun resetToIdle() {
        stopCameraHardware()
        userRequestedCamera = false
        startNonce = 0
        phase = ScannerPhase.Idle
        errorMessage = null
    }

    DisposableEffect(shouldRunCamera) {
        if (!shouldRunCamera) {
            stopCameraHardware()
            onDispose { }
        } else {
            val readyListener: (Event) -> Unit = {
                phase = ScannerPhase.Scanning
                errorMessage = null
            }
            val scanListener: (Event) -> Unit = { event ->
                onCodeScanned(kineticQrReadDetail(event))
            }
            val errorListener: (Event) -> Unit = { event ->
                val message = kineticQrReadDetail(event)
                errorMessage = message
                phase = ScannerPhase.Error
                onError(message)
            }
            window.addEventListener("kinetic-qr-ready", readyListener)
            window.addEventListener("kinetic-qr-scanned", scanListener)
            window.addEventListener("kinetic-qr-error", errorListener)
            onDispose {
                window.removeEventListener("kinetic-qr-ready", readyListener)
                window.removeEventListener("kinetic-qr-scanned", scanListener)
                window.removeEventListener("kinetic-qr-error", errorListener)
                stopCameraHardware()
                windowBounds = null
            }
        }
    }

    var initialStartDone by remember(shouldRunCamera, startNonce) { mutableStateOf(false) }

    LaunchedEffect(shouldRunCamera, windowBounds, startNonce) {
        if (shouldRunCamera && windowBounds != null && startNonce > 0 && !initialStartDone) {
            initialStartDone = true
            phase = ScannerPhase.Starting
            errorMessage = null
            ensureOverlay(elementId, windowBounds!!, density)
            kineticQrStart(elementId)
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            resetToIdle()
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    if (coordinates.size.width <= 0 || coordinates.size.height <= 0) return@onGloballyPositioned
                    val position = coordinates.positionInWindow()
                    val next = WindowBounds(
                        left = position.x,
                        top = position.y,
                        width = coordinates.size.width.toFloat(),
                        height = coordinates.size.height.toFloat()
                    )
                    val current = windowBounds
                    if (current == null ||
                        kotlin.math.abs(current.left - next.left) > 1f ||
                        kotlin.math.abs(current.top - next.top) > 1f ||
                        kotlin.math.abs(current.width - next.width) > 1f ||
                        kotlin.math.abs(current.height - next.height) > 1f
                    ) {
                        windowBounds = next
                        if (shouldRunCamera && startNonce > 0 && (phase == ScannerPhase.Scanning || phase == ScannerPhase.Starting)) {
                            ensureOverlay(elementId, next, density)
                        }
                    }
                }
        ) {
            when {
                !isActive -> CameraOffPlaceholder("Cámara desactivada. Activá el chip «Camera on» para escanear.")
                phase == ScannerPhase.Idle && !userRequestedCamera -> CameraIdlePrompt(
                    onStart = {
                        userRequestedCamera = true
                        startNonce++
                    }
                )
                phase == ScannerPhase.Starting -> CameraLoadingPrompt()
                phase == ScannerPhase.Error -> CameraErrorPrompt(
                    message = errorMessage ?: "No se pudo usar la cámara.",
                    onRetry = {
                        errorMessage = null
                        stopCameraHardware()
                        userRequestedCamera = true
                        startNonce++
                    },
                    onCancel = {
                        resetToIdle()
                    }
                )
                else -> CameraScanningPrompt()
            }
        }
    }
}

@Composable
private fun CameraIdlePrompt(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Escáner QR",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "El navegador va a pedir permiso para usar la cámara. Apuntá al código QR que muestra el miembro en su celular.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceDim,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.Black)
        ) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text("Abrir cámara y escanear", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CameraLoadingPrompt() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = TealPrimary)
        Spacer(Modifier.height(16.dp))
        Text(
            "Iniciando cámara…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Si aparece un cartel del navegador, elegí «Permitir» para continuar.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceDim,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CameraScanningPrompt() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Escaneando… Centrá el QR del miembro dentro del recuadro.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CameraErrorPrompt(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.VideocamOff,
            contentDescription = null,
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Cámara no disponible",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceDim,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.Black)
        ) {
            Text("Reintentar", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCancel) {
            Text("Cancelar", color = Color.White)
        }
    }
}

@Composable
private fun CameraOffPlaceholder(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.VideocamOff,
            contentDescription = null,
            tint = OnSurfaceDim,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceDim,
            textAlign = TextAlign.Center
        )
    }
}

private data class WindowBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

/**
 * Crea o actualiza el div overlay en el DOM con la posición exacta del composable.
 *
 * En lugar de usar `density` directamente, obtenemos el rect CSS real del canvas de Compose
 * y calculamos la escala correcta. Esto maneja casos donde:
 * - El canvas no está en (0,0)
 * - `density` de Compose != window.devicePixelRatio
 * - Hay cualquier transform CSS sobre el canvas
 */
private fun ensureOverlay(elementId: String, bounds: WindowBounds, density: Float) {
    val overlay = (document.getElementById(elementId) as? HTMLDivElement)
        ?: (document.createElement("div") as HTMLDivElement).also { div ->
            div.id = elementId
            document.body!!.appendChild(div)
        }

    // Obtener posición y tamaño CSS real del canvas de Compose
    val canvasCssLeft  = kineticCanvasCssLeft().toFloat()
    val canvasCssTop   = kineticCanvasCssTop().toFloat()
    val canvasCssW     = kineticCanvasCssWidth().toFloat()
    val canvasCssH     = kineticCanvasCssHeight().toFloat()
    val canvasPhysW    = kineticCanvasPhysWidth().toFloat()
    val canvasPhysH    = kineticCanvasPhysHeight().toFloat()

    // Factor de escala: píxeles físicos del canvas → píxeles CSS del viewport.
    // bounds.left/top/width/height están en píxeles físicos del canvas (= Compose px).
    val scaleX = if (canvasPhysW > 0f) canvasCssW / canvasPhysW else (1f / density.coerceAtLeast(1f))
    val scaleY = if (canvasPhysH > 0f) canvasCssH / canvasPhysH else (1f / density.coerceAtLeast(1f))

    val cssLeft   = canvasCssLeft + bounds.left   * scaleX
    val cssTop    = canvasCssTop  + bounds.top    * scaleY
    val cssWidth  = bounds.width  * scaleX
    val cssHeight = bounds.height * scaleY

    val style = overlay.style
    style.position        = "fixed"
    style.left            = "${cssLeft}px"
    style.top             = "${cssTop}px"
    style.width           = "${cssWidth}px"
    style.height          = "${cssHeight}px"
    style.zIndex          = "100"
    style.borderRadius    = "20px"   // Coincidir con el Surface de Compose
    style.backgroundColor = "#000000"
    style.setProperty("overflow",       "hidden")
    style.setProperty("pointer-events", "none")
    // NO setear display/flex/align aquí — lo maneja el JS bridge
}

private fun removeOverlay(elementId: String) {
    document.getElementById(elementId)?.remove()
}

