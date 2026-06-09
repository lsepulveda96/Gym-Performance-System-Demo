package com.gym.frontend.ui.shared

/**
 * Generador de código QR.
 * Utiliza expect/actual para llamar a una librería de JavaScript real en el frontend.
 */
expect object SimpleQRGenerator {
    fun generate(content: String): List<List<Boolean>>
}
