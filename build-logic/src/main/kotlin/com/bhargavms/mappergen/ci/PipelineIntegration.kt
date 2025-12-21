package com.bhargavms.mappergen.ci

import org.gradle.api.Project

/**
 * Integration Stage - Test sample project as end-to-end verification
 */
object PipelineIntegration {
    fun registerTasks(project: Project) {
        // Main integration stage task
        project.tasks.register("pipelineIntegration") {
            group = "CI"
            description = "Integration Stage: Test sample project as end-to-end verification"

            dependsOn(":sample:build", ":sample:test", ":sample:run")
        }
    }
}
