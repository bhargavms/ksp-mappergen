rootProject.name = "integrationTests"

// Reference the main project to consume its modules
includeBuild("..")
// Make the ksp-test-runner plugin available to this build
includeBuild("../ksp-test-runner")

// Share version catalog from main project
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
