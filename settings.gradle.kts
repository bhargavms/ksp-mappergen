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

// KSP test runner plugin as an included build for reuse/publishing
includeBuild("ksp-test-runner")
