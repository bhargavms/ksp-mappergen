package com.example.sample

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RuntimeBehaviorTest {
    @Nested
    inner class FuzzyMatching {
        @Test
        fun `FUZZY matched properties carry values at runtime`() {
            val source =
                Network.FuzzySourceDto(
                    usrname = "alice",
                    emial = "alice@example.com",
                )

            val result = mapFuzzySourceDtoToFuzzyTarget(source)

            assertNotNull(result)
            assertEquals("alice", result?.username)
            assertEquals("alice@example.com", result?.email)
        }

        @Test
        fun `FUZZY mapper returns null for null input`() {
            assertNull(mapFuzzySourceDtoToFuzzyTarget(null))
        }
    }

    @Nested
    inner class EnumDefaults {
        @Test
        fun `nullable enum uses first declared entry as default at runtime`() {
            val source = Network.PartialAccountDto(status = null)

            val result = mapPartialAccountDtoToPartialAccount(source)

            assertNotNull(result)
            assertEquals(Domain.PartialStatus.ACTIVE, result?.status)
        }
    }

    @Nested
    inner class Collections {
        @Test
        fun `empty source list maps to empty target list`() {
            val source = Network.TaggedListDto(tags = emptyList())

            val result = mapTaggedListDtoToTaggedList(source)

            assertNotNull(result)
            assertTrue(result?.tags?.isEmpty() == true)
        }

        @Test
        fun `null list elements are dropped by mapNotNull`() {
            val source =
                Network.TaggedListDto(
                    tags =
                        listOf(
                            Network.TagDto(label = "a"),
                            null,
                            Network.TagDto(label = "b"),
                        ),
                )

            val result = mapTaggedListDtoToTaggedList(source)

            assertNotNull(result)
            assertEquals(listOf("a", "b"), result?.tags?.map { it.label })
        }

        @Test
        fun `null input returns null output`() {
            assertNull(mapTaggedListDtoToTaggedList(null))
        }
    }

    @Nested
    inner class NestedNullPropagation {
        @Test
        fun `nullable nested object null propagates to target field`() {
            val person =
                Network.PersonDto(
                    id = "1",
                    name = "Jane",
                    address = null,
                    age = 30,
                )

            val result = mapPersonDtoToPerson(person)

            assertNotNull(result)
            assertNull(result?.address)
        }
    }
}
