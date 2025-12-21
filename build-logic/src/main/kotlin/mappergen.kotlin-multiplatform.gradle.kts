/**
 * Convention plugin for Kotlin Multiplatform library modules.
 * Used for annotations and other code that consumers compile.
 *
 * JVM version is determined by the consumer's project configuration.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    // Targets - let consumers use any of these
    jvm()

    js(IR) {
        browser()
        nodejs()
    }

    // macOS
    macosX64()
    macosArm64()

    // iOS
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Linux & Windows
    linuxX64()
    mingwX64()

    sourceSets {
        commonMain {
            // Common code goes here
        }
    }
}
