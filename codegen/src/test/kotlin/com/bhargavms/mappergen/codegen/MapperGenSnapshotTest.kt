package com.bhargavms.mappergen.codegen

import com.bhargavms.mappergen.testsupport.MapperGenTestSupport
import com.bhargavms.mappergen.testsupport.compileWithMapperGenFromResources
import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MapperGenSnapshotTest {
    private lateinit var compilation: KotlinCompilation
    private lateinit var compilationResult: CompilationResult

    @BeforeAll
    fun compileMappers() {
        compilation =
            compileWithMapperGenFromResources(
                "sources/com/example/Network.kt",
                "sources/com/example/Domain.kt",
                "sources/com/example/Mappers.kt",
            )
        compilationResult = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, compilationResult.exitCode, compilationResult.messages)
    }

    @Nested
    inner class BasicMappers {
        @Test
        fun `UserDto to User mapper matches expected`() {
            MapperGenTestSupport.assertGeneratedMatchesFixture(
                compilation,
                "com/example/mapUserDtoToUser.kt",
                "fixtures/com/example/mapUserDtoToUser.kt",
            )
        }

        @Test
        fun `ProductDto to Product mapper matches expected`() {
            MapperGenTestSupport.assertGeneratedMatchesFixture(
                compilation,
                "com/example/mapProductDtoToProduct.kt",
                "fixtures/com/example/mapProductDtoToProduct.kt",
            )
        }
    }

    @Nested
    inner class TypeMappings {
        @Test
        fun `Primitive fields apply default values when null`() {
            MapperGenTestSupport.assertGeneratedMatchesFixture(
                compilation,
                "com/example/mapPrimitivesDtoToPrimitives.kt",
                "fixtures/com/example/mapPrimitivesDtoToPrimitives.kt",
            )
        }

        @Test
        fun `Enum fields map with defaults`() {
            MapperGenTestSupport.assertGeneratedMatchesFixture(
                compilation,
                "com/example/mapAccountDtoToAccount.kt",
                "fixtures/com/example/mapAccountDtoToAccount.kt",
            )
        }

        @Test
        fun `Collection fields use emptyList defaults`() {
            MapperGenTestSupport.assertGeneratedMatchesFixture(
                compilation,
                "com/example/mapOrderDtoToOrder.kt",
                "fixtures/com/example/mapOrderDtoToOrder.kt",
            )
        }
    }

    @Nested
    inner class NestedStructures {
        @Test
        fun `Nested DTOs generate recursive mappers`() {
            MapperGenTestSupport.assertGeneratedMatchesFixture(
                compilation,
                "com/example/mapAddressDtoToAddress.kt",
                "fixtures/com/example/mapAddressDtoToAddress.kt",
            )
            MapperGenTestSupport.assertGeneratedMatchesFixture(
                compilation,
                "com/example/mapPersonDtoToPerson.kt",
                "fixtures/com/example/mapPersonDtoToPerson.kt",
            )
        }

        @Test
        fun `Deeply nested DTOs generate complex mappers`() {
            MapperGenTestSupport.assertGeneratedMatchesFixture(
                compilation,
                "com/example/mapCompanyDtoToCompany.kt",
                "fixtures/com/example/mapCompanyDtoToCompany.kt",
            )
        }
    }

    @Nested
    inner class SmartPropertyMatching {
        @Test
        fun `Normalized matching maps snake_case to camelCase`() {
            MapperGenTestSupport.assertGeneratedMatchesFixture(
                compilation,
                "com/example/mapSnakeCaseUserDtoToNormalizedUser.kt",
                "fixtures/com/example/mapSnakeCaseUserDtoToNormalizedUser.kt",
            )
        }

        @Test
        fun `Normalized matching generates correct property references`() {
            val content = MapperGenTestSupport.generatedFile(compilation, "com/example/mapSnakeCaseUserDtoToNormalizedUser.kt")
            assertTrue(content.contains("it.user_id"))
            assertTrue(content.contains("userId ="))
        }
    }

    @Nested
    inner class CustomTransformations {
        @Test
        fun `Custom expressions generate inline transformations`() {
            MapperGenTestSupport.assertGeneratedMatchesFixture(
                compilation,
                "com/example/mapCustomerDtoToCustomer.kt",
                "fixtures/com/example/mapCustomerDtoToCustomer.kt",
            )
        }
    }

    @Nested
    inner class CodeQuality {
        @Test
        fun `Null safety preserved with let`() {
            val mapperFiles =
                listOf(
                    "com/example/mapUserDtoToUser.kt",
                    "com/example/mapProductDtoToProduct.kt",
                    "com/example/mapPrimitivesDtoToPrimitives.kt",
                    "com/example/mapAddressDtoToAddress.kt",
                    "com/example/mapPersonDtoToPerson.kt",
                    "com/example/mapOrderDtoToOrder.kt",
                    "com/example/mapAccountDtoToAccount.kt",
                    "com/example/mapCompanyDtoToCompany.kt",
                    "com/example/mapSnakeCaseUserDtoToNormalizedUser.kt",
                    "com/example/mapCustomerDtoToCustomer.kt",
                )
            mapperFiles.forEach { path ->
                val content = MapperGenTestSupport.generatedFile(compilation, path)
                assertTrue(content.contains("input?.let"), "Expected input?.let in $path")
            }
        }

        @Test
        fun `Correct package declaration used`() {
            val fixtures =
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
            fixtures.forEach { fileName ->
                val content = MapperGenTestSupport.generatedFile(compilation, "com/example/$fileName")
                assertTrue(content.contains("package com.example"), "Incorrect package in $fileName")
            }
        }

        @Test
        fun `Function naming follows mapXToY convention`() {
            assertTrue(MapperGenTestSupport.generatedFile(compilation, "com/example/mapUserDtoToUser.kt").contains("fun mapUserDtoToUser"))
            assertTrue(
                MapperGenTestSupport
                    .generatedFile(compilation, "com/example/mapSnakeCaseUserDtoToNormalizedUser.kt")
                    .contains("fun mapSnakeCaseUserDtoToNormalizedUser"),
            )
        }
    }
}
