package com.bhargavms.mappergen.ci

import org.gradle.api.Project

/**
 * Test Stage - Runs all tests and generates reports
 */
object PipelineTest {
    fun registerTasks(project: Project) {
        // Main test stage task
        project.tasks.register("pipelineTest") {
            group = "CI"
            description = "Test Stage: Run all unit tests and generate reports"

            dependsOn(":test:test", ":codegen:test")
        }

        project.tasks.register("pipelineCoverage") {
            group = "CI"
            description = "Coverage Stage: Run processor tests and enforce Kover gate on :codegen"

            dependsOn(":codegen:test", ":codegen:koverVerify")
        }

        // Separate task for integration tests only. integrationTests is an included build, so its
        // tasks are unreachable by project path and have to be referenced through the composite API.
        project.tasks.register("pipelineIntegrationTest") {
            group = "CI"
            description = "Run integration tests (included build)"
            dependsOn(
                project.gradle
                    .includedBuild("integrationTests")
                    .task(":runKspTests"),
            )
        }

        // Report generation task
        project.tasks.register("pipelineReport") {
            group = "CI"
            description = "Report Stage: Generate and prepare test reports for artifacts"

            dependsOn("pipelineTest", "pipelineIntegrationTest", "pipelineCoverage") // Ensure tests run first

            doLast {
                var unitTestSuccess = false
                var integrationTestSuccess = false

                // Check unit test reports
                val unitHtmlReport = project.file("test/build/reports/tests/test/index.html")
                val unitJunitDir = project.file("test/build/test-results/test")

                if (unitHtmlReport.exists()) {
                    println("✅ Unit test HTML report generated at ${unitHtmlReport.absolutePath}")
                    unitTestSuccess = true
                } else {
                    println("⚠️  Unit test HTML report not found at ${unitHtmlReport.absolutePath}")
                }

                if (unitJunitDir.exists() && unitJunitDir.listFiles()?.isNotEmpty() == true) {
                    val xmlFiles = unitJunitDir.listFiles()?.filter { it.name.endsWith(".xml") }?.size ?: 0
                    println("✅ Unit test JUnit XML reports generated ($xmlFiles files) at ${unitJunitDir.absolutePath}")
                    unitTestSuccess = unitTestSuccess && true
                } else {
                    println("⚠️  Unit test JUnit XML reports not found expected at ${unitJunitDir.absolutePath}")
                }

                // Check integration test reports (from included build)
                val integrationHtmlReport = project.file("integrationTests/build/reports/tests/test/index.html")
                val integrationJunitDir = project.file("integrationTests/build/test-results/test")

                if (integrationHtmlReport.exists()) {
                    println("✅ Integration test HTML report generated at ${integrationHtmlReport.absolutePath}")
                    integrationTestSuccess = true
                } else {
                    println("⚠️  Integration test HTML report not found at ${integrationHtmlReport.absolutePath}")
                }

                if (integrationJunitDir.exists() && integrationJunitDir.listFiles()?.isNotEmpty() == true) {
                    val xmlFiles = integrationJunitDir.listFiles()?.filter { it.name.endsWith(".xml") }?.size ?: 0
                    println("✅ Integration test JUnit XML reports generated ($xmlFiles files) at ${integrationJunitDir.absolutePath}")
                    integrationTestSuccess = integrationTestSuccess && true
                } else {
                    println("⚠️  Integration test JUnit XML reports not found expected at ${integrationJunitDir.absolutePath}")
                }

                // Check Kover coverage reports
                val koverHtmlReport = project.file("build/reports/kover/html/index.html")
                if (koverHtmlReport.exists()) {
                    println("✅ Kover HTML report generated at ${koverHtmlReport.absolutePath}")
                } else {
                    println("⚠️  Kover HTML report not found at ${koverHtmlReport.absolutePath}")
                }

                // Summary
                if (unitTestSuccess && integrationTestSuccess) {
                    println("🎉 All test reports generated successfully!")
                } else {
                    println("⚠️  Some test reports are missing. Check the logs above.")
                }
            }
        }
    }
}
