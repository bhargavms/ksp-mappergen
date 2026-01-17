package com.example.sample

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BasicMappingsTest {
    @Test
    fun `test user mapping`() {
        val userDto =
            Network.UserDto(
                id = "user-123",
                name = "John Doe",
                email = "john@example.com",
                age = 30,
            )
        val user = mapUserDtoToUser(userDto)

        assertNotNull(user)
        assertEquals("user-123", user?.id)
        assertEquals("John Doe", user?.name)
        assertEquals("john@example.com", user?.email)
        assertEquals(30, user?.age)
    }

    @Test
    fun `test product mapping`() {
        val productDto =
            Network.ProductDto(
                id = "prod-456",
                title = "Kotlin in Action",
                price = 39.99,
            )
        val product = mapProductDtoToProduct(productDto)

        assertNotNull(product)
        assertEquals("prod-456", product?.id)
        assertEquals("Kotlin in Action", product?.title)
        assertEquals(39.99, product?.price ?: 0.0, 0.001)
    }

    @Test
    fun `test null input returns null`() {
        val user = mapUserDtoToUser(null)
        assertNull(user)

        val product = mapProductDtoToProduct(null)
        assertNull(product)
    }
}
