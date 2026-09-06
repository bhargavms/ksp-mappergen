package com.bhargavms.mappergen.ci

import org.gradle.api.Project

/**
 * Release Stage - Publish artifacts to Maven repository
 */
object PipelineRelease {
    fun registerTasks(project: Project) {
        // Main release/publish task
        project.tasks.register("pipelineReleasePublish") {
            group = "CI"
            description = "Publish Stage: Publish artifacts to Maven repository"

            doLast {
                println(
                    "Upload runs from release-please after it creates a tag, " +
                        "using repository secrets. Not from this task.",
                )
            }
        }
    }
}
