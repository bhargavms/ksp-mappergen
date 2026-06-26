package com.bhargavms.mappergen.ci

import org.gradle.api.Project

/**
 * Quality Stage - Code quality checks and analysis
 */
object PipelineQuality {
    fun registerTasks(project: Project) {
        // Main quality stage task
        project.tasks.register("pipelineQuality") {
            group = "CI"
            description = "Quality Stage: Code quality checks and analysis"

            dependsOn("pipelineLint", "pipelineSecurity")
        }

        // Linting sub-task
        project.tasks.register("pipelineLint") {
            group = "CI"
            description = "Linting: Check code style and formatting"

            dependsOn("ktlintCheck")
        }

        // Security sub-task (dependency check disabled until NVD API key is configured;
        // dependencyCheckAnalyze is not configuration-cache compatible)
        project.tasks.register("pipelineSecurity") {
            group = "CI"
            description = "Security: Dependency vulnerability checks (currently disabled)"
        }
    }
}
