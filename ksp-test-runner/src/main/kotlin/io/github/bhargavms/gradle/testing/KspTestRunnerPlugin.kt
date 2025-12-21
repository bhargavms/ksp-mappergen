package io.github.bhargavms.gradle.testing

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Gradle plugin entry point for the KSP test runner DSL.
 */
class KspTestRunnerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                "kspTests",
                KspTestExtension::class.java,
                project.layout.buildDirectory,
            )
        val kspTestFilter = project.providers.gradleProperty("kspTestFilter")

        project.tasks.register("runKspTests", RunKspTestsTask::class.java) {
            group = "verification"
            description = "Run KSP integration tests defined with the kspTests DSL"
            // The DSL executes build-script closures; mark not CC-compatible to avoid serialization issues.
            notCompatibleWithConfigurationCache("runKspTests executes build-script-defined test closures")

            // Wire filter property from Gradle property (avoids capturing Project in task action)
            filterProperty.convention(kspTestFilter)
        }
    }
}

abstract class RunKspTestsTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val filterProperty: Property<String>

    @TaskAction
    fun runTests() {
        val extension = project.extensions.getByType(KspTestExtension::class.java)
        val filter = filterProperty.orNull?.toRegex()
        runKspTests(extension, filter)
    }
}
