pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    // Downloads a matching JDK when a toolchain (JDK 17 to compile, JDK 21 to run tests) is missing.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "MuslimTV"

include(":app")
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:datastore")
include(":core:data")
include(":core:database")
include(":core:media")
include(":core:downloads")
include(":feature:downloads")
include(":feature:quran")
include(":feature:azkar")
include(":feature:tools")
include(":feature:onboarding")
include(":feature:home")
include(":feature:prayer")
include(":feature:settings")
