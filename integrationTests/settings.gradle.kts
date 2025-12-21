rootProject.name = "integrationTests"

// Reference the main project to consume its modules
includeBuild("..")

// Share version catalog from main project
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
