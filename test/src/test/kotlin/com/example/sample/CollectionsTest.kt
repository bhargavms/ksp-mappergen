package com.example.sample

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CollectionsTest {
    @Test
    fun `test order with collections mapping`() {
        val orderDto =
            Network.OrderDto(
                id = "order-789",
                items = listOf("Laptop", "Mouse", "Keyboard"),
                quantities = listOf(1, 2, 1),
                total = 1299.99,
            )
        val order = mapOrderDtoToOrder(orderDto)

        assertNotNull(order)
        assertEquals("order-789", order?.id)
        assertEquals(1299.99, order?.total ?: 0.0, 0.001)

        assertNotNull(order?.items)
        assertEquals(3, order?.items?.size)
        assertEquals("Laptop", order?.items?.get(0))
        assertEquals("Mouse", order?.items?.get(1))
        assertEquals("Keyboard", order?.items?.get(2))

        assertNotNull(order?.quantities)
        assertEquals(3, order?.quantities?.size)
        assertEquals(1, order?.quantities?.get(0))
        assertEquals(2, order?.quantities?.get(1))
        assertEquals(1, order?.quantities?.get(2))
    }

    @Test
    fun `test empty collections mapping`() {
        val emptyOrderDto =
            Network.OrderDto(
                id = "order-empty",
                items = emptyList(),
                quantities = emptyList(),
                total = 0.0,
            )
        val emptyOrder = mapOrderDtoToOrder(emptyOrderDto)

        assertNotNull(emptyOrder)
        assertEquals("order-empty", emptyOrder?.id)
        assertEquals(0.0, emptyOrder?.total ?: 0.0, 0.001)
        assertNotNull(emptyOrder?.items)
        assertTrue(emptyOrder?.items?.isEmpty() == true)
        assertNotNull(emptyOrder?.quantities)
        assertTrue(emptyOrder?.quantities?.isEmpty() == true)
    }

    @Test
    fun `test null collections default to empty list`() {
        val orderDto =
            Network.OrderDto(
                id = "order-null",
                items = null,
                quantities = null,
                total = 100.0,
            )
        val order = mapOrderDtoToOrder(orderDto)

        assertNotNull(order)
        assertEquals("order-null", order?.id)
        assertEquals(100.0, order?.total ?: 0.0, 0.001)
        assertNotNull(order?.items)
        assertTrue(order?.items?.isEmpty() == true) // Should default to emptyList()
        assertNotNull(order?.quantities)
        assertTrue(order?.quantities?.isEmpty() == true) // Should default to emptyList()
    }
}
