package com.bhargavms.mappergen.ci

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * CI Pipeline Plugin - Main entrypoint that registers all CI pipeline tasks.
 *
 * This plugin organizes CI tasks into pipeline stages for better structure
 * and maintainability. All tasks are grouped under the "CI" group.
 */
class CiPipelinePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Register pipeline stages
        PipelineBuild.registerTasks(project)
        PipelineTest.registerTasks(project)
        PipelineQuality.registerTasks(project)
        PipelineIntegration.registerTasks(project)
        PipelineRelease.registerTasks(project)

        // Register main pipelines that compose the stages
        registerMainPipelines(project)
    }

    private fun registerMainPipelines(project: Project) {
        // Main CI pipeline: build → test → quality → integration
        project.tasks.register("pipeline") {
            group = "CI"
            description = "Complete CI pipeline: build → test → quality → integration"

            dependsOn("pipelineBuild", "pipelineTest", "pipelineQuality", "pipelineIntegration")
        }

        // Release pipeline: full checks + publish
        project.tasks.register("pipelineRelease") {
            group = "CI"
            description = "Release pipeline: full checks + publish (for release branches/tags)"

            dependsOn("pipeline", "pipelineReleasePublish")
        }
    }
}
