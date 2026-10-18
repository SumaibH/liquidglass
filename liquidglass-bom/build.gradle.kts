plugins {
    `java-platform`
    alias(libs.plugins.mavenPublish)
}

// The BOM: one version for every liquidglass artifact, as Compose's and Firebase's BOMs do.
//   implementation(platform("dev.shayxo.liquidglass:liquidglass-bom:<version>"))
//   implementation("dev.shayxo.liquidglass:liquidglass")
//   implementation("dev.shayxo.liquidglass:liquidglass-material3")
dependencies {
    constraints {
        api(project(":liquidglass"))
        api(project(":liquidglass-material3"))
    }
}

mavenPublishing {
    publishToMavenCentral()
    val hasSigningKey = listOf("signingInMemoryKey", "signing.keyId").any { providers.gradleProperty(it).isPresent }
    if (hasSigningKey) signAllPublications()
}
