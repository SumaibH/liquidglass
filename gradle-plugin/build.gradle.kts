plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "dev.shayxo.liquidglass"
// The plugin adds the library at its own version: keep them in step (root gradle.properties VERSION_NAME).
version = providers.fileContents(layout.projectDirectory.file("../gradle.properties")).asText
    .map { text -> text.lineSequence().first { it.startsWith("VERSION_NAME=") }.substringAfter('=').trim() }.get()

gradlePlugin {
    plugins {
        create("liquidglass") {
            id = "dev.shayxo.liquidglass"
            implementationClass = "dev.shayxo.liquidglass.gradle.LiquidGlassPlugin"
            displayName = "Liquid Glass for Compose"
            description = "Adds liquidglass (and liquidglass-material3 when the project uses Material 3) to a Compose project."
        }
    }
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

// BuildVersion.VALUE: the version the plugin adds, written at build time.
val generateVersion by tasks.registering {
    val out = layout.buildDirectory.dir("generated/version")
    val v = version.toString()
    inputs.property("version", v)
    outputs.dir(out)
    doLast {
        val f = out.get().file("dev/shayxo/liquidglass/gradle/BuildVersion.kt").asFile
        f.parentFile.mkdirs()
        f.writeText("package dev.shayxo.liquidglass.gradle\n\ninternal object BuildVersion { const val VALUE = \"$v\" }\n")
    }
}
kotlin.sourceSets.named("main") { kotlin.srcDir(generateVersion) }
