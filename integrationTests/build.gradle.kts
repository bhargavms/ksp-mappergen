import io.github.bhargavms.gradle.testing.Assertions
import org.gradle.kotlin.dsl.dependencies
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

private fun File.prepareIncrementalWorkspace(
    fixtureDir: File,
    repoRoot: File,
): File {
    if (exists()) {
        deleteRecursively()
    }
    fixtureDir.copyRecursively(this, overwrite = true)
    val repoPath = repoRoot.absolutePath.replace('\\', '/')
    resolve("settings.gradle.kts").writeText(
        """
        pluginManagement {
            includeBuild("$repoPath/build-logic")
            repositories {
                mavenCentral()
                gradlePluginPortal()
            }
        }

        dependencyResolutionManagement {
            repositories {
                mavenCentral()
            }
            versionCatalogs {
                create("libs") {
                    from(files("$repoPath/gradle/libs.versions.toml"))
                }
            }
        }

        rootProject.name = "incremental-fixture"
        includeBuild("$repoPath")
        """.trimIndent(),
    )
    return this
}

private fun File.generatedUserMapper(): File = resolve("build/generated/ksp/main/kotlin/com/example/mapUserDtoToUser.kt")

private fun File.runKsp(vararg extraArgs: String) =
    GradleRunner
        .create()
        .withProjectDir(this)
        .withArguments(listOf("kspKotlin", "--no-build-cache") + extraArgs.toList())
        .forwardOutput()
        .build()

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    `java-test-fixtures`
    id("io.github.bhargavms.ksp-test-runner")
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    create("success") {
        kotlin {
            srcDir("src/success/kotlin")
        }
    }

    create("error") {
        kotlin {
            srcDir("src/error/kotlin")
        }
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    source(sourceSets["success"].kotlin)
}

dependencies {
    ksp("com.bhargavms.mappergen:mappergen")
    compileOnly("com.bhargavms.mappergen:annotations")
    add("successCompileOnly", "com.bhargavms.mappergen:annotations")
    add("errorCompileOnly", "com.bhargavms.mappergen:annotations")
    add("kspError", "com.bhargavms.mappergen:mappergen")
}

kspTests {
    suite("MapperGen Gradle Smoke Tests") {
        lateinit var generatedDir: File
        lateinit var expectedUserMapper: String

        beforeAll {
            generatedDir =
                layout.buildDirectory
                    .dir("generated/ksp/success/kotlin/com/example")
                    .get()
                    .asFile
            expectedUserMapper =
                file("src/testFixtures/kotlin/com/example/mapUserDtoToUser.kt")
                    .readText()
            Assertions.assertFileExists(generatedDir, "KSP generation failed: no files generated")
        }

        test("published mappergen coordinates generate mappers via kspKotlin") {
            val userMapper = generatedDir.resolve("mapUserDtoToUser.kt")
            Assertions.assertFileExists(userMapper, "Expected mapUserDtoToUser.kt from Gradle KSP")
            Assertions.assertContentEquals(expectedUserMapper, userMapper.readText())
        }

        test("success source set generates expected mapper file count") {
            val generatedFiles =
                generatedDir
                    .listFiles()
                    ?.filter { it.isFile && it.extension == "kt" }
                    ?.map { it.name }
                    ?.toSet()
                    ?: emptySet()
            Assertions.assertEquals(10, generatedFiles.size, "Expected 10 generated mapper files, found: $generatedFiles")
        }

        test("Gradle KSP output preserves null-safe mapper signature") {
            val content = generatedDir.resolve("mapUserDtoToUser.kt").readText()
            Assertions.assertContains(content, "input?.let", "Expected null-safe mapper body")
            Assertions.assertMatches(
                content,
                Regex("public fun mapUserDtoToUser\\(input: .+\\?\\):"),
                "Expected public nullable mapper signature",
            )
        }
    }

    suite("Error Compilation Smoke Tests") {
        test("Nullable nested object to non-nullable target shows helpful error via kspErrorKotlin") {
            val result =
                GradleRunner
                    .create()
                    .withProjectDir(rootDir)
                    .withArguments("errorClasses", "--rerun-tasks")
                    .forwardOutput()
                    .buildAndFail()

            val kspTask = result.task(":kspErrorKotlin")
            Assertions.assertNotNull(kspTask, "kspErrorKotlin task should run")
            Assertions.assertNotEquals(
                TaskOutcome.SUCCESS,
                kspTask!!.outcome,
                "Expected kspErrorKotlin to fail for nullable->non-nullable mapping",
            )

            val output = result.output
            Assertions.assertContains(output, "NullableNestedObjectError.kt")
            Assertions.assertContains(output, "address")
        }
    }

    suite("Incremental Compilation E2E") {
        test("DTO change regenerates affected mapper on incremental ksp run") {
            val workspace =
                layout.buildDirectory
                    .dir("incremental-e2e/dto-change")
                    .get()
                    .asFile
                    .prepareIncrementalWorkspace(file("incremental-fixture"), rootDir.parentFile)

            workspace.runKsp()
            val generatedMapper = workspace.generatedUserMapper()
            Assertions.assertFileExists(generatedMapper, "Initial KSP run should generate mapUserDtoToUser.kt")

            val initialContent = generatedMapper.readText()
            Assertions.assertContains(initialContent, "name = it.name")
            Assertions.assertNotContains(initialContent, "nickname")

            workspace
                .resolve("src/main/kotlin/com/example/Network.kt")
                .writeText(
                    """
                    package com.example

                    object Network {
                        data class UserDto(
                            val id: String?,
                            val name: String?,
                            val email: String?,
                            val age: Int?,
                            val nickname: String?,
                        )
                    }
                    """.trimIndent(),
                )
            workspace
                .resolve("src/main/kotlin/com/example/Domain.kt")
                .writeText(
                    """
                    package com.example

                    object Domain {
                        data class User(
                            val id: String,
                            val name: String,
                            val email: String,
                            val age: Int,
                            val nickname: String,
                        )
                    }
                    """.trimIndent(),
                )

            val result = workspace.runKsp()
            val kspOutcome = result.task(":kspKotlin")?.outcome
            Assertions.assertTrue(
                kspOutcome == TaskOutcome.SUCCESS || kspOutcome == TaskOutcome.UP_TO_DATE,
                "Expected kspKotlin to succeed after DTO change, was $kspOutcome",
            )

            val updatedContent = generatedMapper.readText()
            Assertions.assertContains(updatedContent, "nickname = it.nickname")
            Assertions.assertNotEquals(initialContent, updatedContent, "Mapper should be regenerated after DTO change")
        }

        test("unrelated source change leaves generated mapper unchanged on incremental ksp run") {
            val workspace =
                layout.buildDirectory
                    .dir("incremental-e2e/unrelated-change")
                    .get()
                    .asFile
                    .prepareIncrementalWorkspace(file("incremental-fixture"), rootDir.parentFile)

            workspace.runKsp()
            val generatedMapper = workspace.generatedUserMapper()
            Assertions.assertFileExists(generatedMapper, "Initial KSP run should generate mapUserDtoToUser.kt")

            val initialContent = generatedMapper.readText()
            val mainFile = workspace.resolve("src/main/kotlin/com/example/Main.kt")
            mainFile.writeText(
                mainFile
                    .readText()
                    .replace(
                        "println(user?.name)",
                        "println(user?.name) // unrelated comment for incremental test",
                    ),
            )

            val result = workspace.runKsp()
            val kspOutcome = result.task(":kspKotlin")?.outcome
            Assertions.assertTrue(
                kspOutcome == TaskOutcome.SUCCESS ||
                    kspOutcome == TaskOutcome.UP_TO_DATE ||
                    kspOutcome == TaskOutcome.FROM_CACHE,
                "Expected kspKotlin to succeed after unrelated change, was $kspOutcome",
            )

            Assertions.assertContentEquals(
                initialContent,
                generatedMapper.readText(),
                "Unrelated source edits should not regenerate isolating mapper output",
            )
        }
    }
}

tasks.named("runKspTests") {
    dependsOn("kspKotlin")
    dependsOn("compileKotlin")
}
