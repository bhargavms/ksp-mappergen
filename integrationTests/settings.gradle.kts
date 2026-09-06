buildscript {
    dependencies {
        classpath(gradleTestKit())
    }
}

rootProject.name = "integrationTests"

// Reference the main project to consume its modules.
// Published artifactId is mappergen-annotations; project path stays :annotations.
includeBuild("..") {
    dependencySubstitution {
        substitute(module("io.github.bhargavms:mappergen-annotations"))
            .using(project(":annotations"))
    }
}
// Make the ksp-test-runner plugin available to this build
includeBuild("../ksp-test-runner")

// Share version catalog from main project
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
