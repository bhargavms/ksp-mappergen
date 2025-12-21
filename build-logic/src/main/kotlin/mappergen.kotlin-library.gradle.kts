/**
 * Convention plugin for Kotlin JVM library modules.
 * Used for KSP processor and codegen (must be JVM).
 *
 * JVM version is determined by the consumer's project configuration.
 */
plugins {
    id("org.jetbrains.kotlin.jvm")
}
