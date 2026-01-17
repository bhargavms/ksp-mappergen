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
    // Success tests - should compile successfully
    create("success") {
        kotlin {
            srcDir("src/success/kotlin")
        }
    }

    // Error tests - should fail compilation with helpful errors
    create("error") {
        kotlin {
            srcDir("src/error/kotlin")
        }
    }
}

// Configure main compilation to use success source set
tasks.named<KotlinCompile>("compileKotlin") {
    source(sourceSets["success"].kotlin)
}

// Configure KSP to process success sources
dependencies {
    ksp("com.bhargavms.mappergen:mappergen")
    compileOnly("com.bhargavms.mappergen:annotations")

    // Success source set dependencies
    add("successCompileOnly", "com.bhargavms.mappergen:annotations")

    // Error source set dependencies
    add("errorCompileOnly", "com.bhargavms.mappergen:annotations")
    add("kspError", "com.bhargavms.mappergen:mappergen")
}

kspTests {
    suite("MapperGen Integration Tests") {
        lateinit var generatedDir: File
        lateinit var expectedFiles: Map<String, String>

        fun generatedFile(fileName: String): File = generatedDir.resolve("com/example/$fileName")

        fun assertGeneratedContent(fileName: String) {
            val expected =
                Assertions.assertNotNull(
                    expectedFiles[fileName],
                    "Fixture content missing for $fileName",
                )
            val file = generatedFile(fileName)
            Assertions.assertFileExists(file, "Expected generated file not found: $fileName")
            Assertions.assertContentEquals(
                expected,
                file.readText(),
                "Generated file content doesn't match expected: $fileName",
            )
        }

        beforeAll {
            generatedDir =
                layout.buildDirectory
                    .dir("generated/ksp/success/kotlin")
                    .get()
                    .asFile
            val fixturesDir = file("src/testFixtures/kotlin/com/example")
            expectedFiles =
                fixturesDir
                    .listFiles()
                    ?.filter { it.isFile && it.extension == "kt" }
                    ?.associate { it.name to it.readText() }
                    ?: emptyMap()

            Assertions.assertFileExists(generatedDir, "KSP generation failed: no files generated")
        }

        suite("KSP Generation") {

            test("generates all expected mapper files") {
                val generatedFiles =
                    generatedDir
                        .resolve("com/example")
                        .listFiles()
                        ?.filter { it.isFile && it.extension == "kt" }
                        ?.map { it.name }
                        ?.toSet()
                        ?: emptySet()

                val expectedFileNames = expectedFiles.keys
                Assertions.assertTrue(generatedFiles.isNotEmpty(), "No generated mapper files found")
                Assertions.assertEquals(
                    expectedFileNames.size,
                    generatedFiles.size,
                    "File count mismatch: Expected ${expectedFileNames.size}, but found ${generatedFiles.size}\n" +
                        "Expected: ${expectedFileNames.joinToString()}\n" +
                        "Generated: ${generatedFiles.joinToString()}",
                )

                expectedFileNames.forEach { fileName ->
                    Assertions.assertTrue(
                        generatedFiles.contains(fileName),
                        "Expected mapper function not generated: $fileName",
                    )
                }
            }
        }

        suite("Basic Mappers") {

            test("UserDto to User mapper matches expected") {
                assertGeneratedContent("mapUserDtoToUser.kt")
            }

            test("ProductDto to Product mapper matches expected") {
                assertGeneratedContent("mapProductDtoToProduct.kt")
            }
        }

        suite("Type Mappings") {

            test("Primitive fields apply default values when null") {
                assertGeneratedContent("mapPrimitivesDtoToPrimitives.kt")
            }

            test("Enum fields map with defaults") {
                assertGeneratedContent("mapAccountDtoToAccount.kt")
            }

            test("Collection fields use emptyList defaults") {
                assertGeneratedContent("mapOrderDtoToOrder.kt")
            }
        }

        suite("Nested Structures") {

            test("Nested DTOs generate recursive mappers") {
                assertGeneratedContent("mapAddressDtoToAddress.kt")
                assertGeneratedContent("mapPersonDtoToPerson.kt")
            }

            test("Deeply nested DTOs generate complex mappers") {
                assertGeneratedContent("mapCompanyDtoToCompany.kt")
            }
        }

        suite("Smart Property Matching") {

            test("Normalized matching maps snake_case to camelCase") {
                assertGeneratedContent("mapSnakeCaseUserDtoToNormalizedUser.kt")
            }

            test("Normalized matching generates correct property references") {
                val content = generatedFile("mapSnakeCaseUserDtoToNormalizedUser.kt").readText()
                // Verify snake_case source properties are correctly referenced
                Assertions.assertContains(content, "it.user_id", "Should reference snake_case source property user_id")
                Assertions.assertContains(content, "it.user_name", "Should reference snake_case source property user_name")
                Assertions.assertContains(content, "it.email_address", "Should reference snake_case source property email_address")
                Assertions.assertContains(content, "it.created_at", "Should reference snake_case source property created_at")
                // Verify camelCase target properties are correctly assigned
                Assertions.assertContains(content, "userId =", "Should assign to camelCase target property userId")
                Assertions.assertContains(content, "userName =", "Should assign to camelCase target property userName")
                Assertions.assertContains(content, "emailAddress =", "Should assign to camelCase target property emailAddress")
                Assertions.assertContains(content, "createdAt =", "Should assign to camelCase target property createdAt")
            }
        }

        suite("Custom Transformations") {

            test("Custom expressions generate inline transformations") {
                assertGeneratedContent("mapCustomerDtoToCustomer.kt")
            }

            test("Custom expression for fullName concatenation") {
                val content = generatedFile("mapCustomerDtoToCustomer.kt").readText()
                Assertions.assertContains(
                    content,
                    "fullName = (it.firstName.orEmpty()) + \" \" + (it.lastName.orEmpty())",
                    "Should generate fullName concatenation expression",
                )
            }

            test("Custom expression for age calculation") {
                val content = generatedFile("mapCustomerDtoToCustomer.kt").readText()
                Assertions.assertContains(
                    content,
                    "java.time.Year.now().value",
                    "Should include Year.now() in age calculation",
                )
                Assertions.assertContains(
                    content,
                    "it.birthYear",
                    "Should reference birthYear in age calculation",
                )
            }

            test("Custom expression for address joining") {
                val content = generatedFile("mapCustomerDtoToCustomer.kt").readText()
                Assertions.assertContains(
                    content,
                    "listOfNotNull",
                    "Should use listOfNotNull for address parts",
                )
                Assertions.assertContains(
                    content,
                    "joinToString()",
                    "Should use joinToString for address",
                )
            }
        }

        suite("Code Quality") {

            test("Null safety preserved with ?.let") {
                val mapperFiles =
                    listOf(
                        "mapUserDtoToUser.kt",
                        "mapProductDtoToProduct.kt",
                        "mapPrimitivesDtoToPrimitives.kt",
                        "mapAddressDtoToAddress.kt",
                        "mapPersonDtoToPerson.kt",
                        "mapOrderDtoToOrder.kt",
                        "mapAccountDtoToAccount.kt",
                        "mapCompanyDtoToCompany.kt",
                        "mapSnakeCaseUserDtoToNormalizedUser.kt",
                        "mapCustomerDtoToCustomer.kt",
                    )

                mapperFiles.forEach { fileName ->
                    val content = generatedFile(fileName).readText()
                    Assertions.assertContains(
                        content,
                        "input?.let",
                        "Null safety pattern 'input?.let' not found in $fileName",
                    )
                }
            }

            test("Correct package declaration used") {
                expectedFiles.keys.forEach { fileName ->
                    val content = generatedFile(fileName).readText()
                    Assertions.assertMatches(
                        content,
                        Regex("package\\s+com\\.example"),
                        "Incorrect package declaration in $fileName. Expected: 'package com.example'",
                    )
                }
            }

            test("Public nullable function signatures generated") {
                val functionSignatures =
                    mapOf(
                        "mapUserDtoToUser.kt" to "mapUserDtoToUser",
                        "mapProductDtoToProduct.kt" to "mapProductDtoToProduct",
                        "mapPrimitivesDtoToPrimitives.kt" to "mapPrimitivesDtoToPrimitives",
                        "mapAddressDtoToAddress.kt" to "mapAddressDtoToAddress",
                        "mapPersonDtoToPerson.kt" to "mapPersonDtoToPerson",
                        "mapOrderDtoToOrder.kt" to "mapOrderDtoToOrder",
                        "mapAccountDtoToAccount.kt" to "mapAccountDtoToAccount",
                        "mapCompanyDtoToCompany.kt" to "mapCompanyDtoToCompany",
                        "mapSnakeCaseUserDtoToNormalizedUser.kt" to "mapSnakeCaseUserDtoToNormalizedUser",
                        "mapCustomerDtoToCustomer.kt" to "mapCustomerDtoToCustomer",
                    )

                functionSignatures.forEach { (fileName, functionName) ->
                    val content = generatedFile(fileName).readText()
                    // Use DOTALL flag to match across newlines (for long signatures that wrap)
                    Assertions.assertMatches(
                        content,
                        Regex("public fun $functionName\\(input: .+\\?\\):[\\s\\S]*?\\?\\s*=", RegexOption.DOT_MATCHES_ALL),
                        "Incorrect function signature in $fileName. Expected public nullable function with nullable input/output",
                    )
                }
            }
        }

        suite("Error Handling") {

            test("Graceful error when expected file missing") {
                Assertions.assertThrows<AssertionError> {
                    val file = generatedFile("nonExistentMapper.kt")
                    Assertions.assertFileExists(
                        file,
                        "Expected generated file not found: nonExistentMapper.kt",
                    )
                }
            }

            test("No unexpected generated files") {
                val generatedFiles =
                    generatedDir
                        .resolve("com/example")
                        .listFiles()
                        ?.filter { it.isFile && it.extension == "kt" }
                        ?.map { it.name }
                        ?.toSet()
                        ?: emptySet()

                val expectedFileNames = expectedFiles.keys
                val extraFiles = generatedFiles - expectedFileNames
                Assertions.assertTrue(
                    extraFiles.isEmpty(),
                    "Found ${extraFiles.size} unexpected generated files: ${extraFiles.joinToString()}",
                )
            }
        }
    }

    suite("Error Compilation Tests") {
        test("Nullable nested object to non-nullable target shows helpful error") {
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
            Assertions.assertContains(
                output,
                "NullableNestedObjectError.kt",
                "Error should reference the source file",
            )
            Assertions.assertContains(
                output,
                "address",
                "Error should mention the problematic property 'address'",
            )
        }
    }
}

// Ensure KSP/compile run before tests
tasks.named("runKspTests") {
    dependsOn("kspKotlin")
    dependsOn("compileKotlin")
}
