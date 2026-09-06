package com.bhargavms.mappergen.codegen

import com.bhargavms.mappergen.compiler.MapperGenProcessor
import com.bhargavms.mappergen.testsupport.compileWithMapperGenFromResources
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.configureKsp
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.OutputStream

@OptIn(ExperimentalCompilerApi::class)
class MapperGenIncrementalDepsTest {
    @BeforeEach
    fun clearRecordings() {
        RecordedDependencies.recordings.clear()
    }

    @Test
    fun `generated files declare non-empty isolating source dependencies`() {
        val compilation =
            compileWithMapperGenFromResources(
                "sources/com/example/Network.kt",
                "sources/com/example/Domain.kt",
                "sources/com/example/Mappers.kt",
            ).apply {
                configureKsp {
                    symbolProcessorProviders.clear()
                    symbolProcessorProviders.add(RecordingMapperGenProcessorProvider())
                }
            }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        assertTrue(
            RecordedDependencies.recordings.isNotEmpty(),
            "Expected createNewFile to be called with Dependencies",
        )

        RecordedDependencies.recordings.forEach { recording ->
            assertFalse(recording.aggregating, "Mapper outputs should be isolating")
            assertTrue(
                recording.originatingFileNames.isNotEmpty(),
                "Expected originating source files for incremental compilation",
            )
        }

        val userMapperDeps =
            RecordedDependencies.recordings.single { it.fileName == "mapUserDtoToUser" }
        assertTrue(userMapperDeps.originatingFileNames.contains("Mappers.kt"))
        assertTrue(userMapperDeps.originatingFileNames.contains("Network.kt"))
        assertTrue(userMapperDeps.originatingFileNames.contains("Domain.kt"))
    }

    @Test
    fun `nested mapper types include nested type source files`() {
        val compilation =
            compileWithMapperGenFromResources(
                "sources/com/example/Network.kt",
                "sources/com/example/Domain.kt",
                "sources/com/example/Mappers.kt",
            ).apply {
                configureKsp {
                    symbolProcessorProviders.clear()
                    symbolProcessorProviders.add(RecordingMapperGenProcessorProvider())
                }
            }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val personMapperDeps =
            RecordedDependencies.recordings.single { it.fileName == "mapPersonDtoToPerson" }

        assertTrue(
            personMapperDeps.originatingFileNames.contains("Mappers.kt"),
            "Mapper declaration file should be tracked",
        )
        assertTrue(
            personMapperDeps.originatingFileNames.contains("Network.kt"),
            "PersonDto source file should be tracked for nested AddressDto mapping",
        )
        assertTrue(
            personMapperDeps.originatingFileNames.contains("Domain.kt"),
            "Person source file should be tracked for nested Address mapping",
        )
    }
}

private object RecordedDependencies {
    val recordings = mutableListOf<RecordedCreateNewFileCall>()
}

private data class RecordedCreateNewFileCall(
    val aggregating: Boolean,
    val originatingFileNames: List<String>,
    val packageName: String,
    val fileName: String,
)

private class RecordingCodeGenerator(
    delegate: CodeGenerator,
) : CodeGenerator by delegate {
    private val delegateRef = delegate

    override fun createNewFile(
        dependencies: Dependencies,
        packageName: String,
        fileName: String,
        extensionName: String,
    ): OutputStream {
        RecordedDependencies.recordings.add(
            RecordedCreateNewFileCall(
                aggregating = dependencies.aggregating,
                originatingFileNames = dependencies.originatingFiles.map { it.fileName },
                packageName = packageName,
                fileName = fileName,
            ),
        )
        return delegateRef.createNewFile(dependencies, packageName, fileName, extensionName)
    }
}

private class RecordingMapperGenProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val recordingCodeGenerator = RecordingCodeGenerator(environment.codeGenerator)
        return MapperGenProcessor(recordingCodeGenerator, environment.logger)
    }
}
