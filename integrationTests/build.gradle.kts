import io.github.bhargavms.gradle.testing.Assertions
import org.gradle.kotlin.dsl.dependencies
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

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
}

tasks.named("runKspTests") {
    dependsOn("kspKotlin")
    dependsOn("compileKotlin")
}
