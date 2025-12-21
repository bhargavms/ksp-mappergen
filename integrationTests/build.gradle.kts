import org.gradle.kotlin.dsl.dependencies

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    `java-test-fixtures`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

// Consume library as external dependency (simulates real usage)
dependencies {
    ksp("com.bhargavms.mappergen:mappergen")
    compileOnly("com.bhargavms.mappergen:annotations")

    // Test fixtures
    testFixturesImplementation("com.bhargavms.mappergen:annotations")
}

// Helper function to load expected content from testFixtures
fun loadExpectedContent(): Map<String, String> {
    val testFixturesDir = file("src/testFixtures/kotlin/com/example")
    return testFixturesDir
        .listFiles()
        ?.filter { it.isFile && it.extension == "kt" }
        ?.associate { it.name to it.readText().trim() }
        ?: emptyMap()
}

// Helper function to validate exact file content
fun validateGeneratedFile(
    generatedDir: File,
    fileName: String,
    expectedContent: String,
) {
    val file = generatedDir.resolve("com/example/$fileName")
    if (!file.exists()) {
        throw GradleException("❌ TC-INTEGRATION-001: Expected generated file not found: $fileName")
    }

    val actualContent = file.readText().trim()
    if (actualContent != expectedContent) {
        println("❌ Content mismatch in $fileName")
        println("Expected:")
        println(expectedContent)
        println("Actual:")
        println(actualContent)
        throw GradleException("❌ Generated file content doesn't match expected: $fileName")
    }
}

// Helper function to validate null safety patterns in generated code
fun validateNullSafety(
    generatedDir: File,
    fileName: String,
) {
    val file = generatedDir.resolve("com/example/$fileName")
    if (!file.exists()) {
        throw GradleException("❌ Expected generated file not found: $fileName")
    }

    val content = file.readText()
    if (!content.contains("input?.let")) {
        throw GradleException("❌ Null safety pattern 'input?.let' not found in $fileName")
    }
}

// Helper function to validate package declaration
fun validatePackageDeclaration(
    generatedDir: File,
    fileName: String,
    expectedPackage: String = "com.example",
) {
    val file = generatedDir.resolve("com/example/$fileName")
    if (!file.exists()) {
        throw GradleException("❌ Expected generated file not found: $fileName")
    }

    val content = file.readText()
    val packagePattern = Regex("package $expectedPackage")
    if (!packagePattern.containsMatchIn(content)) {
        throw GradleException("❌ Incorrect package declaration in $fileName. Expected: 'package $expectedPackage'")
    }
}

// Helper function to validate function signature
fun validateFunctionSignature(
    generatedDir: File,
    fileName: String,
    expectedFunctionName: String,
) {
    val file = generatedDir.resolve("com/example/$fileName")
    if (!file.exists()) {
        throw GradleException("❌ Expected generated file not found: $fileName")
    }

    val content = file.readText()
    val signaturePattern = Regex("public fun $expectedFunctionName\\(input: .+\\?\\): .+\\?")
    if (!signaturePattern.containsMatchIn(content)) {
        throw GradleException("❌ Incorrect function signature in $fileName. Expected public nullable function with nullable input/output")
    }
}

// ==================== GIVEN: KSP Processing ====================
// Given MapperGen annotations in source code, when KSP runs, then all expected mapper functions are generated
tasks.register("givenMapperAnnotations_whenKspRuns_thenAllExpectedMappersGenerated") {
    group = "verification"
    description = "Verify all expected mapper functions are generated from @Mapper annotations"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile

        if (!generatedDir.exists()) {
            throw GradleException("❌ KSP generation failed: No files generated")
        }

        val expectedFiles = loadExpectedContent()
        val generatedFiles =
            generatedDir
                .resolve("com/example")
                .listFiles()
                ?.filter { it.isFile && it.extension == "kt" }
                ?.map { it.name }
                ?.toSet()
                ?: emptySet()

        val expectedFileNames = expectedFiles.keys.toSet()

        if (generatedFiles.size != expectedFileNames.size) {
            throw GradleException(
                "❌ File count mismatch: Expected ${expectedFileNames.size} mapper functions, but found ${generatedFiles.size}\n" +
                    "Expected: ${expectedFileNames.joinToString()}\n" +
                    "Generated: ${generatedFiles.joinToString()}",
            )
        }

        expectedFileNames.forEach { fileName ->
            if (!generatedFiles.contains(fileName)) {
                throw GradleException("❌ Expected mapper function not generated: $fileName")
            }
        }

        println("✅ All ${expectedFileNames.size} expected mapper functions generated")
    }
}

// ==================== GIVEN: Basic DTOs ====================
// Given simple DTOs with basic types, when mapped with @Mapper, then basic mapper functions match expected code
tasks.register("givenSimpleDtos_whenMapped_thenBasicMappersMatchExpected") {
    group = "verification"
    description = "Validate basic mapper functions generate correct code for simple DTOs"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile
        val expectedFiles = loadExpectedContent()

        validateGeneratedFile(generatedDir, "mapUserDtoToUser.kt", expectedFiles["mapUserDtoToUser.kt"]!!)
        validateGeneratedFile(generatedDir, "mapProductDtoToProduct.kt", expectedFiles["mapProductDtoToProduct.kt"]!!)

        println("✅ Basic mapper functions validated")
    }
}

// ==================== GIVEN: Type Mappings ====================
// Given primitive type fields in DTOs, when mapped with @Mapper, then default values are applied for nulls
tasks.register("givenPrimitiveTypes_whenMapped_thenDefaultValuesApplied") {
    group = "verification"
    description = "Validate primitive types generate appropriate default values when null"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile
        val expectedFiles = loadExpectedContent()

        validateGeneratedFile(generatedDir, "mapPrimitivesDtoToPrimitives.kt", expectedFiles["mapPrimitivesDtoToPrimitives.kt"]!!)

        println("✅ Primitive types mapping validated")
    }
}

// ==================== GIVEN: Nested Structures ====================
// Given nested DTO structures, when mapped with @Mapper, then recursive mapper calls are generated
tasks.register("givenNestedDtos_whenMapped_thenRecursiveMappersGenerated") {
    group = "verification"
    description = "Validate nested objects generate recursive mapping function calls"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile
        val expectedFiles = loadExpectedContent()

        validateGeneratedFile(generatedDir, "mapAddressDtoToAddress.kt", expectedFiles["mapAddressDtoToAddress.kt"]!!)
        validateGeneratedFile(generatedDir, "mapPersonDtoToPerson.kt", expectedFiles["mapPersonDtoToPerson.kt"]!!)

        println("✅ Nested objects mapping validated")
    }
}

// Given collection fields in DTOs, when mapped with @Mapper, then emptyList defaults are applied for nulls
tasks.register("givenCollectionFields_whenMapped_thenEmptyListDefaultsApplied") {
    group = "verification"
    description = "Validate collection fields generate emptyList defaults when null"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile
        val expectedFiles = loadExpectedContent()

        validateGeneratedFile(generatedDir, "mapOrderDtoToOrder.kt", expectedFiles["mapOrderDtoToOrder.kt"]!!)

        println("✅ Collection fields mapping validated")
    }
}

// Given enum fields in DTOs, when mapped with @Mapper, then valueOf mapping is generated with defaults
tasks.register("givenEnumFields_whenMapped_thenValueOfMappingGenerated") {
    group = "verification"
    description = "Validate enum fields generate valueOf mapping with appropriate defaults"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile
        val expectedFiles = loadExpectedContent()

        validateGeneratedFile(generatedDir, "mapAccountDtoToAccount.kt", expectedFiles["mapAccountDtoToAccount.kt"]!!)

        println("✅ Enum fields mapping validated")
    }
}

// Given deeply nested DTO structures, when mapped with @Mapper, then complex recursive mappers are generated
tasks.register("givenDeeplyNestedDtos_whenMapped_thenComplexMappersGenerated") {
    group = "verification"
    description = "Validate deeply nested structures generate complex recursive mapping functions"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile
        val expectedFiles = loadExpectedContent()

        validateGeneratedFile(generatedDir, "mapCompanyDtoToCompany.kt", expectedFiles["mapCompanyDtoToCompany.kt"]!!)

        println("✅ Complex nested structures mapping validated")
    }
}

// ==================== GIVEN: Code Quality ====================
// Given nullable input to mapper functions, when mapper is called, then null safety is preserved with ?.let
tasks.register("givenNullableInput_whenMapperCalled_thenNullSafetyPreserved") {
    group = "verification"
    description = "Validate generated mapper functions preserve null safety with ?.let pattern"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile

        // Test all generated mapper functions for null safety
        val mapperFunctions =
            listOf(
                "mapUserDtoToUser.kt",
                "mapProductDtoToProduct.kt",
                "mapPrimitivesDtoToPrimitives.kt",
                "mapAddressDtoToAddress.kt",
                "mapPersonDtoToPerson.kt",
                "mapOrderDtoToOrder.kt",
                "mapAccountDtoToAccount.kt",
                "mapCompanyDtoToCompany.kt",
            )

        mapperFunctions.forEach { fileName ->
            validateNullSafety(generatedDir, fileName)
        }

        println("✅ Null safety validated in all generated mapper functions")
    }
}

// Given generated mapper functions, when inspected, then correct package declaration is used
tasks.register("givenGeneratedMappers_whenInspected_thenCorrectPackageDeclared") {
    group = "verification"
    description = "Validate all generated mapper functions declare correct package"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile

        // Test all generated mapper functions for package declaration
        val mapperFunctions =
            listOf(
                "mapUserDtoToUser.kt",
                "mapProductDtoToProduct.kt",
                "mapPrimitivesDtoToPrimitives.kt",
                "mapAddressDtoToAddress.kt",
                "mapPersonDtoToPerson.kt",
                "mapOrderDtoToOrder.kt",
                "mapAccountDtoToAccount.kt",
                "mapCompanyDtoToCompany.kt",
            )

        mapperFunctions.forEach { fileName ->
            validatePackageDeclaration(generatedDir, fileName)
        }

        println("✅ Package declarations validated in all generated mapper functions")
    }
}

// Given generated mapper functions, when inspected, then function signatures are correct
tasks.register("givenGeneratedMappers_whenInspected_thenFunctionSignaturesCorrect") {
    group = "verification"
    description = "Validate generated mapper functions have correct public nullable signatures"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile

        // Test function signatures - extract function names from filenames
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
            )

        functionSignatures.forEach { (fileName, functionName) ->
            validateFunctionSignature(generatedDir, fileName, functionName)
        }

        println("✅ Function signatures validated in all generated mapper functions")
    }
}

// ==================== GIVEN: Error Handling ====================
// Given missing fixture file, when validated, then graceful error is provided
tasks.register("givenMissingFixtureFile_whenValidated_thenGracefulError") {
    group = "verification"
    description = "Validate helpful error message when expected fixture file is missing"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile

        try {
            validateGeneratedFile(generatedDir, "nonExistentMapper.kt", "dummy content")
            throw GradleException("❌ Expected validation to fail for missing file")
        } catch (e: GradleException) {
            if (!e.message?.contains("Expected generated file not found")!!) {
                throw GradleException("❌ Error message doesn't indicate missing file: ${e.message}")
            }
        }

        println("✅ Graceful error handling validated for missing fixture files")
    }
}

// Given extra generated file, when validated, then warning is logged
tasks.register("givenExtraGeneratedFile_whenValidated_thenWarningLogged") {
    group = "verification"
    description = "Validate warning is logged when unexpected files are generated"
    dependsOn("compileKotlin")

    doLast {
        val generatedDir =
            layout.buildDirectory
                .dir("generated/ksp/main/kotlin")
                .get()
                .asFile
        val expectedFiles = loadExpectedContent()
        val generatedFiles =
            generatedDir
                .resolve("com/example")
                .listFiles()
                ?.filter { it.isFile && it.extension == "kt" }
                ?.map { it.name }
                ?.toSet()
                ?: emptySet()

        val expectedFileNames = expectedFiles.keys.toSet()
        val extraFiles = generatedFiles - expectedFileNames

        if (extraFiles.isNotEmpty()) {
            throw GradleException("❌ ERROR: Found ${extraFiles.size} unexpected generated files: ${extraFiles.joinToString()}")
        } else {
            println("✅ No unexpected files found in generated output")
        }
    }
}

// Main integration test task - runs all validation subtasks
tasks.register("integrationTest") {
    group = "verification"
    description = "Run all integration tests to validate MapperGen code generation"

    dependsOn(
        "givenMapperAnnotations_whenKspRuns_thenAllExpectedMappersGenerated",
        "givenSimpleDtos_whenMapped_thenBasicMappersMatchExpected",
        "givenPrimitiveTypes_whenMapped_thenDefaultValuesApplied",
        "givenNestedDtos_whenMapped_thenRecursiveMappersGenerated",
        "givenCollectionFields_whenMapped_thenEmptyListDefaultsApplied",
        "givenEnumFields_whenMapped_thenValueOfMappingGenerated",
        "givenDeeplyNestedDtos_whenMapped_thenComplexMappersGenerated",
        "givenNullableInput_whenMapperCalled_thenNullSafetyPreserved",
        "givenGeneratedMappers_whenInspected_thenCorrectPackageDeclared",
        "givenGeneratedMappers_whenInspected_thenFunctionSignaturesCorrect",
        "givenMissingFixtureFile_whenValidated_thenGracefulError",
        "givenExtraGeneratedFile_whenValidated_thenWarningLogged",
    )

    doLast {
        println("✅ All integration tests passed")
    }
}
