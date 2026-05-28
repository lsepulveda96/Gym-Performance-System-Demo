package com.gym.frontend.ui.config

/**
 * Platform-specific environment values.
 *
 * On web (wasmJs), these are sourced from `window` globals injected in `index.html`.
 */
expect object PlatformEnv {
    fun get(key: String): String?
    fun locationOrigin(): String?
    fun hostname(): String?
}

