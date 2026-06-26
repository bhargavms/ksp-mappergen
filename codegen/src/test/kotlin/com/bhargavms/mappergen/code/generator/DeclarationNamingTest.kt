package com.bhargavms.mappergen.code.generator

import com.bhargavms.mappergen.testsupport.MapperGenTestSupport
import com.bhargavms.mappergen.testsupport.compileWithMapperGen
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeclarationNamingTest {
    @Test
    fun `getMapFunctionName uses nested generic type names`() {
        val compilation =
            compileWithMapperGen(
                SourceFile.kotlin(
                    "Types.kt",
                    """
                    package com.example.naming

                    object Network {
                        data class UserDto(val id: String?)
                        data class ItemDto(val users: List<UserDto>?)
                    }

                    object Domain {
                        data class User(val id: String)
                        data class Item(val users: List<User>)
                    }
                    """.trimIndent(),
                ),
                SourceFile.kotlin(
                    "Mappers.kt",
                    """
                    package com.example.naming

                    import com.bhargavms.mappergen.annotations.Mapper

                        interface Mappers {
                            @Mapper
                            fun mapUser(value: Network.UserDto): Domain.User

                            @Mapper
                            fun mapItem(value: Network.ItemDto): Domain.Item
                        }
                    """.trimIndent(),
                ),
            )

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val itemMapper = MapperGenTestSupport.generatedFile(compilation, "com/example/naming/mapItemDtoToItem.kt")
        val userMapper = MapperGenTestSupport.generatedFile(compilation, "com/example/naming/mapUserDtoToUser.kt")

        assertTrue(itemMapper.contains("fun mapItemDtoToItem"))
        assertTrue(userMapper.contains("fun mapUserDtoToUser"))
        assertTrue(itemMapper.contains("mapUserDtoToUser(it)"))
    }
}
