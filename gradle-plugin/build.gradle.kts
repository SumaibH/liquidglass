plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    // Keep in step with mavenPublish in ../gradle/libs.versions.toml (this is a separate build).
    id("com.vanniktech.maven.publish") version "0.37.0"
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

// Published to Maven Central with the library, under the same key: the plugin marker
// (dev.shayxo.liquidglass:dev.shayxo.liquidglass.gradle.plugin) lets `plugins { id(...) }` resolve it from
// mavenCentral(), which new Android and KMP projects already list in pluginManagement.
val rootProperties = providers.fileContents(layout.projectDirectory.file("../gradle.properties")).asText
    .map { text -> text.lineSequence().filter { '=' in it && !it.startsWith("#") }.associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() } }
    .get()
mavenPublishing {
    publishToMavenCentral()
    val hasSigningKey = listOf("signingInMemoryKey", "signing.keyId").any { providers.gradleProperty(it).isPresent }
    if (hasSigningKey) signAllPublications()
    coordinates(group.toString(), "liquidglass-gradle-plugin", version.toString())
    pom {
        name.set("liquidglass-gradle-plugin")
        description.set("The dev.shayxo.liquidglass Gradle plugin: adds liquidglass (and liquidglass-material3 for Material 3 apps) and the liquidGlassAdopt import codemod.")
        inceptionYear.set(rootProperties.getValue("POM_INCEPTION_YEAR"))
        url.set(rootProperties.getValue("POM_URL"))
        licenses {
            license {
                name.set(rootProperties.getValue("POM_LICENSE_NAME"))
                url.set(rootProperties.getValue("POM_LICENSE_URL"))
                distribution.set(rootProperties.getValue("POM_LICENSE_DIST"))
            }
        }
        developers {
            developer {
                id.set(rootProperties.getValue("POM_DEVELOPER_ID"))
                name.set(rootProperties.getValue("POM_DEVELOPER_NAME"))
                url.set(rootProperties.getValue("POM_DEVELOPER_URL"))
            }
        }
        scm {
            url.set(rootProperties.getValue("POM_SCM_URL"))
            connection.set(rootProperties.getValue("POM_SCM_CONNECTION"))
            developerConnection.set(rootProperties.getValue("POM_SCM_DEV_CONNECTION"))
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
