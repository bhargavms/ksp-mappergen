package com.bhargavms.mappergen.ci

import org.gradle.api.Project

/**
 * Build Stage - Compiles all modules
 */
object PipelineBuild {
    fun registerTasks(project: Project) {
        // Main build stage task
        project.tasks.register("pipelineBuild") {
            group = "CI"
            description = "Build Stage: Compile all JVM modules"

            dependsOn(
                ":codegen:build",
                ":mappergen:build",
                ":test:build",
                ":sample:build",
            )
        }

        // Alternative: build everything including KMP (may fail in CI without Node.js)
        project.tasks.register("pipelineBuildFull") {
            group = "CI"
            description = "Build Stage (all platforms): Compile all modules including KMP (requires Node.js)"

            dependsOn(
                ":annotations:build",
                ":codegen:build",
                ":mappergen:build",
                ":test:build",
                ":sample:build",
            )
        }

        // Utility: clean build
        project.tasks.register("pipelineClean") {
            group = "CI"
            description = "Clean Stage: Clean all build outputs (optional)"

            dependsOn("clean")
        }
    }
}
