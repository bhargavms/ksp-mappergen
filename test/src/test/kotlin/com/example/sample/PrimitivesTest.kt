package com.example.sample

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PrimitivesTest {
    @Test
    fun `test all primitive types mapping`() {
        val primitivesDto =
            Network.PrimitivesDto(
                byteValue = 42,
                shortValue = 1000,
                intValue = 100000,
                longValue = 1000000000L,
                floatValue = 3.14f,
                doubleValue = 2.718,
                booleanValue = true,
                charValue = 'K',
                stringValue = "Hello Kotlin",
            )
        val primitives = mapPrimitivesDtoToPrimitives(primitivesDto)

        assertNotNull(primitives)
        assertEquals(42, primitives?.byteValue)
        assertEquals(1000, primitives?.shortValue)
        assertEquals(100000, primitives?.intValue)
        assertEquals(1000000000L, primitives?.longValue)
        assertEquals(3.14f, primitives?.floatValue ?: 0f, 0.001f)
        assertEquals(2.718, primitives?.doubleValue ?: 0.0, 0.001)
        assertTrue(primitives?.booleanValue == true)
        assertEquals('K', primitives?.charValue)
        assertEquals("Hello Kotlin", primitives?.stringValue)
    }

    @Test
    fun `test primitive defaults for null values`() {
        val partialPrimitives =
            Network.PrimitivesDto(
                byteValue = null,
                shortValue = null,
                intValue = null,
                longValue = null,
                floatValue = null,
                doubleValue = null,
                booleanValue = null,
                charValue = null,
                stringValue = null,
            )
        val defaults = mapPrimitivesDtoToPrimitives(partialPrimitives)

        assertNotNull(defaults)
        assertEquals(0, defaults?.byteValue)
        assertEquals(0, defaults?.shortValue)
        assertEquals(0, defaults?.intValue)
        assertEquals(0L, defaults?.longValue)
        assertEquals(0.0f, defaults?.floatValue ?: 0f, 0.001f)
        assertEquals(0.0, defaults?.doubleValue ?: 0.0, 0.001)
        assertFalse(defaults?.booleanValue == true)
        assertEquals('\u0000', defaults?.charValue) // null char
        assertEquals("", defaults?.stringValue)
    }

    @Test
    fun `test partial nulls use defaults`() {
        val partialPrimitives =
            Network.PrimitivesDto(
                byteValue = 10,
                shortValue = null,
                intValue = 20,
                longValue = null,
                floatValue = 1.5f,
                doubleValue = null,
                booleanValue = true,
                charValue = null,
                stringValue = "test",
            )
        val result = mapPrimitivesDtoToPrimitives(partialPrimitives)

        assertNotNull(result)
        assertEquals(10, result?.byteValue)
        assertEquals(0, result?.shortValue) // default
        assertEquals(20, result?.intValue)
        assertEquals(0L, result?.longValue) // default
        assertEquals(1.5f, result?.floatValue ?: 0f, 0.001f)
        assertEquals(0.0, result?.doubleValue ?: 0.0, 0.001) // default
        assertTrue(result?.booleanValue == true)
        assertEquals('\u0000', result?.charValue) // default
        assertEquals("test", result?.stringValue)
    }
}
