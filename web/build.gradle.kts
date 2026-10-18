import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Atlas Studio in the browser: the same Loupe screen as the desktop and Android apps, compiled to
// WebAssembly and drawn by the library's production shaders through Compose for Web.
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
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
            }
            kotlin.srcDir("../sample/src/main/kotlin/shared")
        }
    }
}
