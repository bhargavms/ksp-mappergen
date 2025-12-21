pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "mappergen"

include(":annotations")
include(":codegen")
include(":mappergen")
include(":test")
include(":sample")

// Integration tests as included build (simulates real-world consumption)
includeBuild("integrationTests")
