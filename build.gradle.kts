plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    id("org.jetbrains.kotlin.multiplatform") version "2.1.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.1.0" apply false
    id("org.jetbrains.compose") version "1.7.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("io.ktor.plugin") version "3.0.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
}
