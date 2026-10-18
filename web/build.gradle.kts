import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// The live demo: Atlas Studio (the same Loupe screen as the desktop and Android apps) and, at #material3,
// a Material 3 app built from liquidglass-material3. WebAssembly, drawn by the production shaders.
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "atlas"
        browser {
            commonWebpackConfig { outputFileName = "atlas.js" }
        }
        binaries.executable()
    }
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":liquidglass"))
                implementation(project(":liquidglass-material3"))
                implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
            }
            kotlin.srcDir("../sample/src/main/kotlin/shared")
        }
    }
}
