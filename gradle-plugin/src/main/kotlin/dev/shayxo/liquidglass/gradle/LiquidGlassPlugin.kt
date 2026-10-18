package dev.shayxo.liquidglass.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * `plugins { id("dev.shayxo.liquidglass") }`: the one line that adds Liquid Glass to a Compose project.
 *
 * It adds `dev.shayxo.liquidglass:liquidglass` at the plugin's own version to `commonMain` in a
 * Kotlin Multiplatform project, or to `implementation` in an Android or JVM one. When the project
 * already uses Material 3 it adds `liquidglass-material3` too. Turn either off in the extension:
 *
 * ```kotlin
 * liquidGlass {
 *     material3.set(false)      // never add liquidglass-material3
 * }
 * ```
 *
 * `./gradlew liquidGlassAdopt` switches an existing app's Material 3 imports to the glass versions once.
 */
class LiquidGlassPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("liquidGlass", LiquidGlassExtension::class.java)
        extension.core.convention(true)
        extension.version.convention(pluginVersion)
        project.tasks.register("liquidGlassAdopt", AdoptTask::class.java) {
            group = "liquid glass"
            description = "Switches Material 3 imports to the Liquid Glass versions (--dry-run to preview)."
            sources.from(project.fileTree("src") { include("**/*.kt") })
            dryRun.convention(false)
        }
        project.afterEvaluate {
            val configuration = listOf("commonMainImplementation", "implementation")
                .firstOrNull { project.configurations.findByName(it) != null }
                ?: return@afterEvaluate project.logger.warn("liquidglass: no commonMain or implementation configuration; nothing added. Apply the Kotlin or Android plugin too.")
            val version = extension.version.get()
            if (extension.core.get()) project.dependencies.add(configuration, "$GROUP:liquidglass:$version")
            val wantsMaterial3 = extension.material3.orNull ?: usesMaterial3(project)
            if (wantsMaterial3) project.dependencies.add(configuration, "$GROUP:liquidglass-material3:$version")
        }
    }

    /** True when any configuration declares Material 3 (Jetpack Compose's or Compose Multiplatform's). */
    private fun usesMaterial3(project: Project): Boolean = project.configurations.any { c ->
        c.dependencies.any { d -> d.name == "material3" && (d.group == "androidx.compose.material3" || d.group == "org.jetbrains.compose.material3") }
    }

    internal companion object {
        const val GROUP = "dev.shayxo.liquidglass"
        val pluginVersion: String = LiquidGlassPlugin::class.java.`package`?.implementationVersion ?: BuildVersion.VALUE
    }
}

/** The `liquidGlass { }` block. */
abstract class LiquidGlassExtension {
    /** Add the core `liquidglass` artifact. True by default. */
    abstract val core: Property<Boolean>
    /** Add `liquidglass-material3`: true or false, or unset to add it only when the project uses Material 3. */
    abstract val material3: Property<Boolean>
    /** The library version to add; the plugin's own by default. */
    abstract val version: Property<String>
}
