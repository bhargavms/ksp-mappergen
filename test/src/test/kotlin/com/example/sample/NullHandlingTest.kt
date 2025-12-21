package com.example.sample

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NullHandlingTest {
    @Test
    fun `test completely null input returns null`() {
        val nullUser = mapUserDtoToUser(null)
        assertNull(nullUser)

        val nullProduct = mapProductDtoToProduct(null)
        assertNull(nullProduct)

        val nullOrder = mapOrderDtoToOrder(null)
        assertNull(nullOrder)
    }

    @Test
    fun `test partial nulls use defaults`() {
        val partialUser =
            Network.UserDto(
                id = "user-789",
                name = null,
                email = null,
                age = null,
            )
        val mappedPartial = mapUserDtoToUser(partialUser)

        assertNotNull(mappedPartial)
        assertEquals("user-789", mappedPartial?.id)
        assertEquals("", mappedPartial?.name) // Default for String
        assertEquals("", mappedPartial?.email) // Default for String
        assertEquals(0, mappedPartial?.age) // Default for Int
    }

    @Test
    fun `test mixed null and non-null values`() {
        val mixedUser =
            Network.UserDto(
                id = "user-mixed",
                name = "John",
                email = null,
                age = 25,
            )
        val mapped = mapUserDtoToUser(mixedUser)

        assertNotNull(mapped)
        assertEquals("user-mixed", mapped?.id)
        assertEquals("John", mapped?.name)
        assertEquals("", mapped?.email) // Default for null String
        assertEquals(25, mapped?.age)
    }
}
