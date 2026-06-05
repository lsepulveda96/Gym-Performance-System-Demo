plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

compose {
    resources {
        publicResClass = true
        packageOfResClass = "gym_system.composeapp.generated.resources"
    }
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(project.projectDir.path + "/src/wasmJsMain/resources")
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.wasm.js)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
    }
}

// Produces a single static folder ready for Netlify/Vercel.
// Output: composeApp/build/webDemo
val prepareWebDemo by tasks.registering(Copy::class) {
    dependsOn("wasmJsBrowserProductionWebpack")

    val outDir = layout.buildDirectory.dir("webDemo")
    into(outDir)

    // HTML + runtime config + extra static resources.
    from(layout.buildDirectory.dir("processedResources/wasmJs/main")) {
        // IMPORTANT: include Compose resources too (images/fonts/etc).
        // If we only include a few files, the production demo will miss assets.
        include("**/*")
    }

    // Webpack bundle output (js + wasm).
    from(layout.buildDirectory.dir("kotlin-webpack/wasmJs/productionExecutable")) {
        include("**/*")
    }
}

