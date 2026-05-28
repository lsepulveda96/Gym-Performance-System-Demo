package com.gym.frontend.ui.config

object AppConfigKeys {
    const val ENV = "__KINETIC_ENV__" // development | production
    const val API_BASE = "__KINETIC_API_BASE__" // e.g. https://api.example.com
    const val DEMO_MODE = "__KINETIC_DEMO_MODE__" // "true" | "false"
}

object AppConfig {
    val env: String = PlatformEnv.get(AppConfigKeys.ENV)?.trim().orEmpty().ifBlank { defaultEnv() }

    val demoMode: Boolean = PlatformEnv.get(AppConfigKeys.DEMO_MODE)
        ?.trim()
        ?.lowercase()
        ?.let { it == "true" || it == "1" || it == "yes" }
        ?: false

    fun apiBaseUrl(): String {
        val fromEnv = PlatformEnv.get(AppConfigKeys.API_BASE)?.trim().orEmpty()
        val raw = if (fromEnv.isNotBlank()) fromEnv else defaultApiBaseUrl()
        return raw.trimEnd('/')
    }

    private fun defaultEnv(): String {
        val host = PlatformEnv.hostname()?.lowercase()
        return if (host == "localhost" || host == "127.0.0.1") "development" else "production"
    }

    private fun defaultApiBaseUrl(): String {
        val host = PlatformEnv.hostname()?.lowercase()
        return if (host == "localhost" || host == "127.0.0.1") {
            // Local backend default for dev only (no code hardcoded in services).
            "http://localhost:8080"
        } else {
            // If frontend and backend are deployed together behind the same origin.
            PlatformEnv.locationOrigin() ?: ""
        }
    }
}

