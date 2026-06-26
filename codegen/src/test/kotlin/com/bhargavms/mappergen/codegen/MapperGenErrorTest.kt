package com.bhargavms.mappergen.codegen

import com.bhargavms.mappergen.testsupport.MapperGenTestSupport
import com.bhargavms.mappergen.testsupport.compileWithMapperGen
import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MapperGenErrorTest {
    @Nested
    inner class CompileErrors {
        @Test
        fun `nullable nested object to non-nullable target fails compilation`() {
            val result =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "Error.kt",
                        """
                        package com.example.errors

                        import com.bhargavms.mappergen.annotations.Mapper

                        data class SourceDto(val address: AddressDto?)
                        data class AddressDto(val street: String, val city: String)
                        data class Target(val address: Address)
                        data class Address(val street: String, val city: String)

                        interface TestMapper {
                            @Mapper
                            fun map(dto: SourceDto): Target
                        }
                        """.trimIndent(),
                    ),
                ).compile()

            assertCompilationFailed(result)
            assertTrue(result.messages.contains("address"))
        }

        @Test
        fun `nullable enum to empty target enum fails compilation`() {
            val result =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "EnumError.kt",
                        """
                        package com.example.errors

                        import com.bhargavms.mappergen.annotations.Mapper

                        enum class SourceStatus { ACTIVE }
                        enum class TargetStatus { }

                        data class SourceDto(val status: SourceStatus?)
                        data class TargetDto(val status: TargetStatus)

                        interface TestMapper {
                            @Mapper
                            fun map(source: SourceDto): TargetDto
                        }
                        """.trimIndent(),
                    ),
                ).compile()

            assertCompilationFailed(result)
            assertTrue(result.messages.contains("status") || result.messages.contains("enum"))
        }

        @Test
        fun `enum missing target entries fails compilation`() {
            val result =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "MissingEnumError.kt",
                        """
                        package com.example.errors

                        import com.bhargavms.mappergen.annotations.Mapper

                        enum class SourceStatus { ACTIVE, INACTIVE, PENDING }
                        enum class TargetStatus { ACTIVE, INACTIVE }

                        data class SourceDto(val status: SourceStatus)
                        data class TargetDto(val status: TargetStatus)

                        interface TestMapper {
                            @Mapper
                            fun map(source: SourceDto): TargetDto
                        }
                        """.trimIndent(),
                    ),
                ).compile()

            assertCompilationFailed(result)
            assertTrue(result.messages.contains("PENDING"))
        }

        @Test
        fun `unknown type with no default fails compilation`() {
            val result =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "UnknownTypeError.kt",
                        """
                        package com.example.errors

                        import com.bhargavms.mappergen.annotations.Mapper
                        import java.util.UUID

                        data class SourceDto(val id: UUID?)
                        data class TargetDto(val id: UUID)

                        interface TestMapper {
                            @Mapper
                            fun map(source: SourceDto): TargetDto
                        }
                        """.trimIndent(),
                    ),
                ).compile()

            assertCompilationFailed(result)
            assertTrue(result.messages.contains("UUID") || result.messages.contains("default value"))
        }

        @Test
        fun `Mapper on class target fails compilation`() {
            val result =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "BadTargetError.kt",
                        """
                        package com.example.errors

                        import com.bhargavms.mappergen.annotations.Mapper

                        data class SourceDto(val id: String?)
                        data class TargetDto(val id: String)

                        @Mapper
                        class BadMapper {
                            fun map(source: SourceDto): TargetDto = TargetDto(source.id ?: "")
                        }
                        """.trimIndent(),
                    ),
                ).compile()

            assertCompilationFailed(result)
            assertTrue(result.messages.contains("function") || result.messages.contains("BadAnnotationTargetException"))
        }

        @Test
        fun `unmatched required property should emit helpful error`() {
            val result =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "UnmatchedPropertyError.kt",
                        """
                        package com.example.errors

                        import com.bhargavms.mappergen.annotations.Mapper

                        data class SourceDto(val id: String?)
                        data class TargetDto(val id: String, val requiredName: String)

                        interface TestMapper {
                            @Mapper
                            fun map(source: SourceDto): TargetDto
                        }
                        """.trimIndent(),
                    ),
                ).compile()

            assertCompilationFailed(result)
            assertTrue(result.messages.contains("requiredName"))
        }

        @Test
        fun `recursive type graph should be guarded with helpful error`() {
            val result =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "RecursiveError.kt",
                        """
                        package com.example.errors

                        import com.bhargavms.mappergen.annotations.Mapper

                        data class NodeDto(val child: NodeDto?)
                        data class Node(val child: Node?)

                        interface TestMapper {
                            @Mapper
                            fun map(source: NodeDto): Node
                        }
                        """.trimIndent(),
                    ),
                ).compile()

            assertCompilationFailed(result)
            assertTrue(result.messages.contains("recursive") || result.messages.contains("cycle"))
        }

        @Test
        fun `map types should be handled explicitly`() {
            val compilation =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "MapGenericError.kt",
                        """
                        package com.example.errors

                        import com.bhargavms.mappergen.annotations.Mapper

                        data class SourceDto(val values: Map<String, Int>?)
                        data class TargetDto(val values: Map<String, Int>)

                        interface TestMapper {
                            @Mapper
                            fun map(source: SourceDto): TargetDto
                        }
                        """.trimIndent(),
                    ),
                )
            val result = compilation.compile()
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

            val content =
                MapperGenTestSupport.generatedFile(compilation, "com/example/errors/mapSourceDtoToTargetDto.kt")
            assertTrue(
                content.contains("values = it.values?.entries?.associate"),
                "Expected explicit Map mapping support",
            )
        }
    }

    private fun assertCompilationFailed(result: CompilationResult) {
        assertNotEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }
}
