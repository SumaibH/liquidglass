import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

// Liquid Glass versions of the Material 3 components: the same names and parameters, so an app swaps
// `import androidx.compose.material3.*` for `import com.wexpa.liquidglass.material3.*`.
kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    android {
        namespace = "com.wexpa.liquidglass.material3"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":liquidglass"))
            api(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(compose.desktop.currentOs)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            // Icons for the showcase renders only (the last published material-icons-core).
            implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    val hasSigningKey = listOf("signingInMemoryKey", "signing.keyId").any { providers.gradleProperty(it).isPresent }
    if (hasSigningKey) signAllPublications()
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty(), sourcesJar = true))
}

tasks.matching { it.name == "checkComposeUiTestConfigurationForWasmJs" }.configureEach { enabled = false }

// runDesktopComposeUiTest (Compose 1.12.1) wraps the test in runTest with the default 60 s timeout and
// ignores its own testTimeout; phone-sized glass renders need longer.
tasks.withType<Test>().configureEach { systemProperty("kotlinx.coroutines.test.default_timeout", "5m") }
