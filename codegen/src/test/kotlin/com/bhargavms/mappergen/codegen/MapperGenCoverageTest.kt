package com.bhargavms.mappergen.codegen

import com.bhargavms.mappergen.testsupport.MapperGenTestSupport
import com.bhargavms.mappergen.testsupport.compileWithMapperGen
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapperGenCoverageTest {
    @Test
    fun `nullable enum to nullable enum uses safe when mapping`() {
        val compilation =
            compileScenario(
                """
                enum class SourceStatus { ACTIVE, INACTIVE }
                enum class TargetStatus { ACTIVE, INACTIVE }
                data class SourceDto(val status: SourceStatus?)
                data class TargetDto(val status: TargetStatus?)
                """.trimIndent(),
                "mapAccount",
                "SourceDto",
                "TargetDto",
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("?.let"))
        assertTrue(content.contains("when (source)"))
    }

    @Test
    fun `non-null enum maps with when branches`() {
        val compilation =
            compileScenario(
                """
                enum class SourceStatus { ACTIVE, INACTIVE }
                enum class TargetStatus { ACTIVE, INACTIVE }
                data class SourceDto(val status: SourceStatus)
                data class TargetDto(val status: TargetStatus)
                """.trimIndent(),
                "mapAccount",
                "SourceDto",
                "TargetDto",
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("when (it.status)"))
    }

    @Test
    fun `same element type collections assign directly`() {
        val compilation =
            compileScenario(
                """
                data class SourceDto(val tags: List<String>?)
                data class TargetDto(val tags: List<String>)
                """.trimIndent(),
                "mapTags",
                "SourceDto",
                "TargetDto",
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("tags = it.tags ?: emptyList()"))
    }

    @Test
    fun `array properties apply defaults when null`() {
        val compilation =
            compileScenario(
                """
                data class SourceDto(val values: Array<Int>?)
                data class TargetDto(val values: Array<Int>)
                """.trimIndent(),
                "mapValues",
                "SourceDto",
                "TargetDto",
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("values = it.values") || content.contains("emptyArray"))
    }

    @Test
    fun `collection element mapper is generated for list of objects`() {
        val compilation =
            compileScenario(
                """
                data class TagDto(val name: String?)
                data class Tag(val name: String)
                data class SourceDto(val tags: List<TagDto>?)
                data class TargetDto(val tags: List<Tag>)
                """.trimIndent(),
                "mapSource",
                "SourceDto",
                "TargetDto",
                extraMappers =
                    """
                    @Mapper
                    fun mapTag(value: TagDto): Tag
                    """.trimIndent(),
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("mapTagDtoToTag"))
    }

    @Test
    fun `property transform with source override uses configured source`() {
        val compilation =
            compileWithMapperGen(
                SourceFile.kotlin(
                    "Types.kt",
                    """
                    package com.example.coverage

                    data class SourceDto(val first: String?, val last: String?)
                    data class TargetDto(val fullName: String)
                    """.trimIndent(),
                ),
                SourceFile.kotlin(
                    "Mappers.kt",
                    """
                    package com.example.coverage

                    import com.bhargavms.mappergen.annotations.Mapper
                    import com.bhargavms.mappergen.annotations.PropertyTransform

                    interface Mappers {
                        @Mapper(
                            transforms = [
                                PropertyTransform(
                                    target = "fullName",
                                    source = "first",
                                    expression = "it.first.orEmpty()",
                                ),
                            ],
                        )
                        fun mapSource(value: SourceDto): TargetDto
                    }
                    """.trimIndent(),
                ),
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("fullName = it.first.orEmpty()"))
    }

    @Test
    fun `nullable nested object to nullable target maps with nested function`() {
        val compilation =
            compileScenario(
                """
                data class AddressDto(val city: String?)
                data class Address(val city: String)
                data class SourceDto(val address: AddressDto?)
                data class TargetDto(val address: Address?)
                """.trimIndent(),
                "mapSource",
                "SourceDto",
                "TargetDto",
                extraMappers =
                    """
                    @Mapper
                    fun mapAddress(value: AddressDto): Address
                    """.trimIndent(),
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("mapAddressDtoToAddress(it.address)"))
    }

    @Test
    fun `different enum names map via when expression`() {
        val compilation =
            compileScenario(
                """
                enum class SourceStatusDto { ACTIVE, INACTIVE }
                enum class TargetStatus { ACTIVE, INACTIVE }
                data class SourceDto(val status: SourceStatusDto)
                data class TargetDto(val status: TargetStatus)
                """.trimIndent(),
                "mapAccount",
                "SourceDto",
                "TargetDto",
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("when (it.status)"))
        assertTrue(content.contains("ACTIVE"))
    }

    @Test
    fun `nullable collection to nullable collection uses safe mapNotNull`() {
        val compilation =
            compileScenario(
                """
                data class TagDto(val name: String?)
                data class Tag(val name: String)
                data class SourceDto(val tags: List<TagDto>?)
                data class TargetDto(val tags: List<Tag>?)
                """.trimIndent(),
                "mapSource",
                "SourceDto",
                "TargetDto",
                extraMappers =
                    """
                    @Mapper
                    fun mapTag(value: TagDto): Tag
                    """.trimIndent(),
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("?.mapNotNull"))
    }

    @Test
    fun `each primitive nullable field gets a default value`() {
        val primitives =
            listOf(
                "byteValue" to "Byte",
                "shortValue" to "Short",
                "intValue" to "Int",
                "longValue" to "Long",
                "floatValue" to "Float",
                "doubleValue" to "Double",
                "booleanValue" to "Boolean",
                "charValue" to "Char",
                "stringValue" to "String",
            )
        primitives.forEach { (field, type) ->
            val compilation =
                compileScenario(
                    """
                    data class SourceDto(val $field: $type?)
                    data class TargetDto(val $field: $type)
                    """.trimIndent(),
                    "mapValue",
                    "SourceDto",
                    "TargetDto",
                )
            assertCompiledOk(compilation)
            val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
            assertTrue(content.contains("$field = it.$field ?:"), "Expected default for $field")
        }
    }

    @Test
    fun `root list mapper generates iterable mapping function`() {
        val compilation =
            compileWithMapperGen(
                SourceFile.kotlin(
                    "Types.kt",
                    """
                    package com.example.coverage

                    data class UserDto(val id: String?)
                    data class User(val id: String)
                    """.trimIndent(),
                ),
                SourceFile.kotlin(
                    "Mappers.kt",
                    """
                    package com.example.coverage

                    import com.bhargavms.mappergen.annotations.Mapper

                    interface Mappers {
                        @Mapper
                        fun mapUser(value: UserDto): User

                        @Mapper
                        fun mapUsers(value: List<UserDto>): List<User>
                    }
                    """.trimIndent(),
                ),
            )
        assertCompiledOk(compilation)
        val usersMapper = MapperGenTestSupport.generatedFile(compilation, "com/example/coverage/mapUserDtoListToUserList.kt")
        assertTrue(usersMapper.contains("mapNotNull"))
        assertTrue(usersMapper.contains("mapUserDtoToUser"))
    }

    @Test
    fun `nullable collection to non-null uses emptyList fallback`() {
        val compilation =
            compileScenario(
                """
                data class TagDto(val name: String?)
                data class Tag(val name: String)
                data class SourceDto(val tags: List<TagDto>?)
                data class TargetDto(val tags: List<Tag>)
                """.trimIndent(),
                "mapSource",
                "SourceDto",
                "TargetDto",
                extraMappers =
                    """
                    @Mapper
                    fun mapTag(value: TagDto): Tag
                    """.trimIndent(),
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("?: emptyList()"))
    }

    @Test
    fun `same enum simple name uses direct assignment path`() {
        val compilation =
            compileScenario(
                """
                enum class Status { ACTIVE, INACTIVE }
                data class SourceDto(val status: Status)
                data class TargetDto(val status: Status)
                """.trimIndent(),
                "mapStatus",
                "SourceDto",
                "TargetDto",
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("when (it.status)") || content.contains("status = it.status"))
    }

    @Test
    fun `non-null collection maps with mapNotNull`() {
        val compilation =
            compileScenario(
                """
                data class TagDto(val name: String?)
                data class Tag(val name: String)
                data class SourceDto(val tags: List<TagDto>)
                data class TargetDto(val tags: List<Tag>)
                """.trimIndent(),
                "mapSource",
                "SourceDto",
                "TargetDto",
                extraMappers =
                    """
                    @Mapper
                    fun mapTag(value: TagDto): Tag
                    """.trimIndent(),
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains(".mapNotNull { mapTagDtoToTag(it) }"))
    }

    @Test
    fun `nullable enum to non-null uses default entry branch`() {
        val compilation =
            compileScenario(
                """
                enum class SourceStatus { ACTIVE, INACTIVE, PENDING }
                enum class TargetStatus { ACTIVE, INACTIVE, PENDING }
                data class SourceDto(val status: SourceStatus?)
                data class TargetDto(val status: TargetStatus)
                """.trimIndent(),
                "mapStatus",
                "SourceDto",
                "TargetDto",
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("null ->") && content.contains("PENDING"))
    }

    @Test
    fun `nullable to nullable properties assign directly`() {
        val compilation =
            compileScenario(
                """
                data class SourceDto(val nickname: String?)
                data class TargetDto(val nickname: String?)
                """.trimIndent(),
                "mapSource",
                "SourceDto",
                "TargetDto",
            )
        assertCompiledOk(compilation)
        val content = generated(compilation, "mapSourceDtoToTargetDto.kt")
        assertTrue(content.contains("nickname = it.nickname"))
        assertTrue(!content.contains("nickname = it.nickname ?:"))
    }

    @Test
    fun `invalid mapper declaration types fail fast`() {
        val result =
            compileWithMapperGen(
                SourceFile.kotlin(
                    "BadTypes.kt",
                    """
                    package com.example.coverage

                    import com.bhargavms.mappergen.annotations.Mapper

                    interface Mappers {
                        @Mapper
                        fun mapValue(value: String): Int
                    }
                    """.trimIndent(),
                ),
            ).compile()
        assertTrue(result.exitCode != KotlinCompilation.ExitCode.OK)
    }

    private fun compileScenario(
        types: String,
        mapperName: String,
        sourceType: String,
        targetType: String,
        extraMappers: String = "",
    ) = compileWithMapperGen(
        SourceFile.kotlin(
            "Types.kt",
            """
            package com.example.coverage

            $types
            """.trimIndent(),
        ),
        SourceFile.kotlin(
            "Mappers.kt",
            """
            package com.example.coverage

            import com.bhargavms.mappergen.annotations.Mapper

            interface Mappers {
                $extraMappers
                @Mapper
                fun $mapperName(value: $sourceType): $targetType
            }
            """.trimIndent(),
        ),
    )

    private fun assertCompiledOk(compilation: com.tschuchort.compiletesting.KotlinCompilation) {
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    private fun generated(
        compilation: com.tschuchort.compiletesting.KotlinCompilation,
        fileName: String,
    ): String = MapperGenTestSupport.generatedFile(compilation, "com/example/coverage/$fileName")
}
