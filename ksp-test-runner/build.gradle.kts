plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("kspTestRunner") {
            id = "io.github.bhargavms.ksp-test-runner"
            displayName = "KSP Test Runner"
            description = "JUnit-like KSP integration test runner DSL with assertions, lifecycle hooks, and colorized reporting."
            implementationClass = "io.github.bhargavms.gradle.testing.KspTestRunnerPlugin"
            tags.set(listOf("ksp", "testing", "kotlin", "gradle-plugin", "dsl"))
        }
    }
}

// pluginBundle {
//    website = "https://github.com/bhargavms/mappergen"
//    vcsUrl = "https://github.com/bhargavms/mappergen"
//    description = "JUnit-like KSP integration test runner DSL with assertions, lifecycle hooks, and colorized reporting."
//    tags = listOf("ksp", "testing", "kotlin", "dsl")
// }

dependencies {
    implementation(libs.kotlin.gradlePlugin)
}
