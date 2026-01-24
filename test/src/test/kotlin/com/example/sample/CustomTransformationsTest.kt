package com.example.sample

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Year

/**
 * Tests for custom property transformations.
 */
class CustomTransformationsTest {
    @Test
    fun `test custom transformation - fullName concatenation`() {
        val customerDto =
            Network.CustomerDto(
                firstName = "John",
                lastName = "Doe",
                birthYear = 1990,
                addressLine1 = "123 Main St",
                addressLine2 = "Apt 4B",
                cityName = "New York",
            )

        val result = mapCustomerDtoToCustomer(customerDto)

        assertNotNull(result)
        assertEquals("John Doe", result?.fullName)
    }

    @Test
    fun `test custom transformation - age calculation`() {
        // Use a fixed expected age and derive birthYear from it to avoid flakiness
        // from Year.now() being called at different instants in test vs mapper
        val expectedAge = 30
        val birthYear = Year.now().value - expectedAge

        val customerDto =
            Network.CustomerDto(
                firstName = "John",
                lastName = "Doe",
                birthYear = birthYear,
                addressLine1 = "123 Main St",
                addressLine2 = null,
                cityName = "New York",
            )

        val result = mapCustomerDtoToCustomer(customerDto)

        assertNotNull(result)
        assertEquals(expectedAge, result?.age)
    }

    @Test
    fun `test custom transformation - fullAddress joining`() {
        val customerDto =
            Network.CustomerDto(
                firstName = "Jane",
                lastName = "Smith",
                birthYear = 1985,
                addressLine1 = "456 Oak Ave",
                addressLine2 = "Suite 100",
                cityName = "Los Angeles",
            )

        val result = mapCustomerDtoToCustomer(customerDto)

        assertNotNull(result)
        // joinToString() uses default ", " separator
        assertEquals("456 Oak Ave, Suite 100, Los Angeles", result?.fullAddress)
    }

    @Test
    fun `test custom transformation - fullAddress with null parts`() {
        val customerDto =
            Network.CustomerDto(
                firstName = "Bob",
                lastName = "Jones",
                birthYear = 2000,
                addressLine1 = "789 Pine Rd",
                addressLine2 = null,
                cityName = "Chicago",
            )

        val result = mapCustomerDtoToCustomer(customerDto)

        assertNotNull(result)
        // joinToString() uses default ", " separator, nulls are filtered by listOfNotNull
        assertEquals("789 Pine Rd, Chicago", result?.fullAddress)
    }

    @Test
    fun `test custom transformation with all nulls`() {
        val customerDto =
            Network.CustomerDto(
                firstName = null,
                lastName = null,
                birthYear = null,
                addressLine1 = null,
                addressLine2 = null,
                cityName = null,
            )

        val result = mapCustomerDtoToCustomer(customerDto)

        assertNotNull(result)
        // orEmpty() returns "" for null, so "" + " " + "" = " "
        assertEquals(" ", result?.fullName)
        // When birthYear is null, mapper uses default 2000; compute expected age the same way
        val expectedDefaultAge = Year.now().value - 2000
        assertEquals(expectedDefaultAge, result?.age)
        assertEquals("", result?.fullAddress) // all nulls filtered out by listOfNotNull
    }

    @Test
    fun `test null input returns null`() {
        val result = mapCustomerDtoToCustomer(null)
        assertNull(result)
    }
}
