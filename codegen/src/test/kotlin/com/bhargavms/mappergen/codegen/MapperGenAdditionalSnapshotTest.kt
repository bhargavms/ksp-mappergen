package com.bhargavms.mappergen.codegen

import com.bhargavms.mappergen.testsupport.MapperGenTestSupport
import com.bhargavms.mappergen.testsupport.compileWithMapperGen
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MapperGenAdditionalSnapshotTest {
    @Nested
    inner class FuzzyMatching {
        @Test
        fun `FUZZY strategy maps typo property names end to end`() {
            val compilation =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "Types.kt",
                        """
                        package com.example.fuzzy

                        object Network {
                            data class TypoDto(
                                val usrname: String?,
                                val emial: String?,
                            )
                        }

                        object Domain {
                            data class User(
                                val username: String,
                                val email: String,
                            )
                        }
                        """.trimIndent(),
                    ),
                    SourceFile.kotlin(
                        "Mappers.kt",
                        """
                        package com.example.fuzzy

                        import com.bhargavms.mappergen.annotations.Mapper
                        import com.bhargavms.mappergen.annotations.MatchingStrategy

                        interface Mappers {
                            @Mapper(matchingStrategy = MatchingStrategy.FUZZY)
                            fun mapUser(value: Network.TypoDto): Domain.User
                        }
                        """.trimIndent(),
                    ),
                )

            val result = compilation.compile()
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

            val content = MapperGenTestSupport.generatedFile(compilation, "com/example/fuzzy/mapTypoDtoToUser.kt")
            assertTrue(content.contains("it.usrname"))
            assertTrue(content.contains("it.emial"))
            assertTrue(content.contains("username ="))
            assertTrue(content.contains("email ="))
        }
    }

    @Nested
    inner class ExactMatching {
        @Test
        fun `EXACT strategy matches case insensitively`() {
            val compilation =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "Types.kt",
                        """
                        package com.example.exact

                        object Network {
                            data class CaseDto(
                                val UserName: String?,
                                val EMAIL: String?,
                            )
                        }

                        object Domain {
                            data class User(
                                val userName: String,
                                val email: String,
                            )
                        }
                        """.trimIndent(),
                    ),
                    SourceFile.kotlin(
                        "Mappers.kt",
                        """
                        package com.example.exact

                        import com.bhargavms.mappergen.annotations.Mapper

                        interface Mappers {
                            @Mapper
                            fun mapUser(value: Network.CaseDto): Domain.User
                        }
                        """.trimIndent(),
                    ),
                )

            val result = compilation.compile()
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

            val content = MapperGenTestSupport.generatedFile(compilation, "com/example/exact/mapCaseDtoToUser.kt")
            assertTrue(content.contains("it.UserName"))
            assertTrue(content.contains("it.EMAIL"))
        }
    }

    @Nested
    inner class CollectionEdgeCases {
        @Test
        fun `list of nested objects generates element mapper calls`() {
            val compilation =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "Types.kt",
                        """
                        package com.example.collections

                        object Network {
                            data class TagDto(val name: String?)
                            data class PostDto(val tags: List<TagDto>?)
                        }

                        object Domain {
                            data class Tag(val name: String)
                            data class Post(val tags: List<Tag>)
                        }
                        """.trimIndent(),
                    ),
                    SourceFile.kotlin(
                        "Mappers.kt",
                        """
                        package com.example.collections

                        import com.bhargavms.mappergen.annotations.Mapper

                        interface Mappers {
                            @Mapper
                            fun mapTag(value: Network.TagDto): Domain.Tag

                            @Mapper
                            fun mapPost(value: Network.PostDto): Domain.Post
                        }
                        """.trimIndent(),
                    ),
                )

            val result = compilation.compile()
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

            val postMapper = MapperGenTestSupport.generatedFile(compilation, "com/example/collections/mapPostDtoToPost.kt")
            val tagMapper = MapperGenTestSupport.generatedFile(compilation, "com/example/collections/mapTagDtoToTag.kt")
            assertTrue(postMapper.contains("mapTagDtoToTag"))
            assertTrue(tagMapper.contains("fun mapTagDtoToTag"))
        }

        @Test
        fun `nested List of Lists should map element types recursively`() {
            val compilation =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "NestedTypes.kt",
                        """
                        package com.example.collections

                        object Network {
                            data class TagDto(val name: String?)
                            data class PostDto(val tags: List<List<TagDto>>?)
                        }

                        object Domain {
                            data class Tag(val name: String)
                            data class Post(val tags: List<List<Tag>>)
                        }
                        """.trimIndent(),
                    ),
                    SourceFile.kotlin(
                        "NestedMappers.kt",
                        """
                        package com.example.collections

                        import com.bhargavms.mappergen.annotations.Mapper

                        interface Mappers {
                            @Mapper
                            fun mapTag(value: Network.TagDto): Domain.Tag

                            @Mapper
                            fun mapPost(value: Network.PostDto): Domain.Post
                        }
                        """.trimIndent(),
                    ),
                )

            val result = compilation.compile()
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

            val postMapper = MapperGenTestSupport.generatedFile(compilation, "com/example/collections/mapPostDtoToPost.kt")
            assertTrue(postMapper.contains("map { inner -> inner.mapNotNull { mapTagDtoToTag(it) } }"))
        }
    }

    @Nested
    inner class EnumDefaults {
        @Test
        fun `enum default should use first declared entry`() {
            val compilation =
                compileWithMapperGen(
                    SourceFile.kotlin(
                        "Types.kt",
                        """
                        package com.example.enums

                        object Network {
                            enum class StatusDto { ACTIVE, INACTIVE, PENDING }
                            data class AccountDto(val status: StatusDto?)
                        }

                        object Domain {
                            enum class Status { ACTIVE, INACTIVE, PENDING }
                            data class Account(val status: Status)
                        }
                        """.trimIndent(),
                    ),
                    SourceFile.kotlin(
                        "Mappers.kt",
                        """
                        package com.example.enums

                        import com.bhargavms.mappergen.annotations.Mapper

                        interface Mappers {
                            @Mapper
                            fun mapAccount(value: Network.AccountDto): Domain.Account
                        }
                        """.trimIndent(),
                    ),
                )

            val result = compilation.compile()
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

            val content = MapperGenTestSupport.generatedFile(compilation, "com/example/enums/mapAccountDtoToAccount.kt")
            assertTrue(
                content.contains("null ->") && content.contains(".Status.ACTIVE"),
                "Expected explicit ACTIVE default for nullable enum",
            )
        }
    }
}
