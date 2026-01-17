package com.example.sample

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NestedObjectsTest {
    @Test
    fun `test address mapping`() {
        val addressDto =
            Network.AddressDto(
                street = "123 Main St",
                city = "San Francisco",
                zipCode = "94102",
                country = "USA",
            )
        val address = mapAddressDtoToAddress(addressDto)

        assertNotNull(address)
        assertEquals("123 Main St", address?.street)
        assertEquals("San Francisco", address?.city)
        assertEquals("94102", address?.zipCode)
        assertEquals("USA", address?.country)
    }

    @Test
    fun `test person with nested address mapping`() {
        val addressDto =
            Network.AddressDto(
                street = "123 Main St",
                city = "San Francisco",
                zipCode = "94102",
                country = "USA",
            )
        val personDto =
            Network.PersonDto(
                id = "person-1",
                name = "Alice",
                address = addressDto,
                age = 28,
            )
        val person = mapPersonDtoToPerson(personDto)

        assertNotNull(person)
        assertEquals("person-1", person?.id)
        assertEquals("Alice", person?.name)
        assertEquals(28, person?.age)

        assertNotNull(person?.address)
        assertEquals("123 Main St", person?.address?.street)
        assertEquals("San Francisco", person?.address?.city)
        assertEquals("94102", person?.address?.zipCode)
        assertEquals("USA", person?.address?.country)
    }

    @Test
    fun `test null nested object`() {
        val personDto =
            Network.PersonDto(
                id = "person-2",
                name = "Bob",
                address = null,
                age = 30,
            )
        // The mapper handles null nested objects by propagating the null value.
        // mapAddressDtoToAddress returns null when input is null, so person.address will be null.
        val person = mapPersonDtoToPerson(personDto)

        assertNotNull(person)
        assertEquals("person-2", person?.id)
        assertEquals("Bob", person?.name)
        assertEquals(30, person?.age)
        assertNull(person?.address)
    }
}
