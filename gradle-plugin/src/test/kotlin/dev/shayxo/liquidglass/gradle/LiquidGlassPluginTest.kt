package dev.shayxo.liquidglass.gradle

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/** The plugin applied to real JVM projects through TestKit: it adds the right artifacts. */
class LiquidGlassPluginTest {
    private fun project(material3: Boolean, extension: String = ""): String {
        val dir = createTempDirectory("lg-plugin").toFile()
        File(dir, "settings.gradle.kts").writeText("rootProject.name = \"probe\"\n")
        File(dir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("jvm") version "2.2.0"
                id("dev.shayxo.liquidglass")
            }
            repositories { mavenCentral() }
            dependencies {
                ${if (material3) "implementation(\"org.jetbrains.compose.material3:material3:1.8.0\")" else ""}
            }
            $extension
            tasks.register("printLiquidGlass") {
                val deps = configurations.getByName("implementation").dependencies.map { "${'$'}{it.group}:${'$'}{it.name}:${'$'}{it.version}" }
                doLast { deps.forEach { println("DEP " + it) } }
            }
        """.trimIndent())
        return GradleRunner.create().withProjectDir(dir).withPluginClasspath()
            .withArguments("printLiquidGlass", "--stacktrace").build().output
    }

    @Test fun addsTheCoreArtifact() {
        val out = project(material3 = false)
        assertContains(out, "DEP dev.shayxo.liquidglass:liquidglass:")
        assertFalse(out.contains("liquidglass-material3"), out)
    }

    @Test fun addsMaterial3WhenTheProjectUsesIt() {
        assertContains(project(material3 = true), "DEP dev.shayxo.liquidglass:liquidglass-material3:")
    }

    @Test fun material3CanBeTurnedOff() {
        assertFalse(project(material3 = true, "liquidGlass { material3.set(false) }").contains("liquidglass-material3"))
    }
}
