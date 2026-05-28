package com.gym.frontend.ui.config

import kotlin.js.JsName

actual object PlatformEnv {
    actual fun get(key: String): String? {
        return kineticGetConfig(key)
    }

    actual fun locationOrigin(): String? = runCatching { kineticGetOrigin() }.getOrNull()

    actual fun hostname(): String? = runCatching { kineticGetHostname() }.getOrNull()
}

@JsName("kineticGetConfig")
private external fun kineticGetConfig(key: String): String?

@JsName("kineticGetOrigin")
private external fun kineticGetOrigin(): String

@JsName("kineticGetHostname")
private external fun kineticGetHostname(): String

