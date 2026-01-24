package com.example.sample

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the smart property matching features.
 */
class SmartMatchingTest {
    @Test
    fun `test normalized matching - snake_case to camelCase`() {
        val snakeCaseDto =
            Network.SnakeCaseUserDto(
                user_id = "user-123",
                user_name = "John Doe",
                email_address = "john@example.com",
                created_at = 1705500000000L,
            )

        val result = mapSnakeCaseUserDtoToNormalizedUser(snakeCaseDto)

        assertNotNull(result)
        assertEquals("user-123", result?.userId)
        assertEquals("John Doe", result?.userName)
        assertEquals("john@example.com", result?.emailAddress)
        assertEquals(1705500000000L, result?.createdAt)
    }

    @Test
    fun `test normalized matching with null values`() {
        val snakeCaseDto =
            Network.SnakeCaseUserDto(
                user_id = null,
                user_name = null,
                email_address = null,
                created_at = null,
            )

        val result = mapSnakeCaseUserDtoToNormalizedUser(snakeCaseDto)

        assertNotNull(result)
        assertEquals("", result?.userId)
        assertEquals("", result?.userName)
        assertEquals("", result?.emailAddress)
        assertEquals(0L, result?.createdAt)
    }

    @Test
    fun `test null input returns null`() {
        val result = mapSnakeCaseUserDtoToNormalizedUser(null)
        assertNull(result)
    }
}
