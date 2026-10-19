rootProject.name = "liquidglass"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// The library, and the app that shows it off. Only the first is published.
include(":liquidglass")
include(":liquidglass-material3")
include(":liquidglass-bom")
// The apps live under apps/ (20 Oct 2026); their project paths stay :sample, :desktop and :web.
include(":sample")
include(":desktop")
include(":web")
project(":sample").projectDir = file("apps/sample")
project(":desktop").projectDir = file("apps/desktop")
project(":web").projectDir = file("apps/web")
