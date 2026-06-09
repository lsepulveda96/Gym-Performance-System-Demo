package com.gym.frontend.ui.shared

import kotlin.js.JsName

@JsName("kineticGenerateQrMatrixString")
private external fun kineticGenerateQrMatrixString(text: String): String

/**
 * Generador de código QR para WasmJs.
 * Llama a una función de JavaScript en window que utiliza qrcode-generator.
 */
actual object SimpleQRGenerator {
    actual fun generate(content: String): List<List<Boolean>> {
        try {
            val res = kineticGenerateQrMatrixString(content)
            val parts = res.split(":")
            val size = parts.getOrNull(0)?.toIntOrNull() ?: 0
            if (size == 0) return emptyList()
            val data = parts.getOrNull(1) ?: return emptyList()
            
            val matrix = mutableListOf<List<Boolean>>()
            var idx = 0
            for (r in 0 until size) {
                val row = mutableListOf<Boolean>()
                for (c in 0 until size) {
                    if (idx < data.length) {
                        row.add(data[idx++] == '1')
                    } else {
                        row.add(false)
                    }
                }
                matrix.add(row)
            }
            return matrix
        } catch (e: Throwable) {
            return emptyList()
        }
    }
}
