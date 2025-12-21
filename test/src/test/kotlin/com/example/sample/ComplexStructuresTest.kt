package com.example.sample

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ComplexStructuresTest {
    @Test
    fun `test company with nested employees and address mapping`() {
        val companyDto =
            Network.CompanyDto(
                id = "company-1",
                name = "Tech Corp",
                employees =
                    listOf(
                        Network.PersonDto(
                            id = "emp-1",
                            name = "Bob",
                            address = Network.AddressDto("456 Oak Ave", "Seattle", "98101", "USA"),
                            age = 32,
                        ),
                        Network.PersonDto(
                            id = "emp-2",
                            name = "Carol",
                            address = Network.AddressDto("789 Pine St", "Portland", "97201", "USA"),
                            age = 29,
                        ),
                    ),
                headquarters = Network.AddressDto("100 Tech Blvd", "San Jose", "95110", "USA"),
            )
        val company = mapCompanyDtoToCompany(companyDto)

        assertNotNull(company)
        assertEquals("company-1", company?.id)
        assertEquals("Tech Corp", company?.name)

        // Test headquarters
        assertNotNull(company?.headquarters)
        assertEquals("100 Tech Blvd", company?.headquarters?.street)
        assertEquals("San Jose", company?.headquarters?.city)
        assertEquals("95110", company?.headquarters?.zipCode)
        assertEquals("USA", company?.headquarters?.country)

        // Test employees collection
        assertNotNull(company?.employees)
        assertEquals(2, company?.employees?.size)

        val firstEmployee = company?.employees?.get(0)
        assertNotNull(firstEmployee)
        assertEquals("emp-1", firstEmployee?.id)
        assertEquals("Bob", firstEmployee?.name)
        assertEquals(32, firstEmployee?.age)
        assertNotNull(firstEmployee?.address)
        assertEquals("456 Oak Ave", firstEmployee?.address?.street)
        assertEquals("Seattle", firstEmployee?.address?.city)

        val secondEmployee = company?.employees?.get(1)
        assertNotNull(secondEmployee)
        assertEquals("emp-2", secondEmployee?.id)
        assertEquals("Carol", secondEmployee?.name)
        assertEquals(29, secondEmployee?.age)
        assertNotNull(secondEmployee?.address)
        assertEquals("789 Pine St", secondEmployee?.address?.street)
        assertEquals("Portland", secondEmployee?.address?.city)
    }

    @Test
    fun `test company with empty employees list`() {
        val companyDto =
            Network.CompanyDto(
                id = "company-2",
                name = "Startup Inc",
                employees = emptyList(),
                headquarters = Network.AddressDto("200 Startup St", "Austin", "78701", "USA"),
            )
        val company = mapCompanyDtoToCompany(companyDto)

        assertNotNull(company)
        assertEquals("company-2", company?.id)
        assertEquals("Startup Inc", company?.name)
        assertNotNull(company?.employees)
        assertTrue(company?.employees?.isEmpty() == true)
    }

    @Test
    fun `test company with null employees defaults to empty list`() {
        val companyDto =
            Network.CompanyDto(
                id = "company-3",
                name = "Solo Corp",
                employees = null,
                headquarters = Network.AddressDto("300 Solo Ave", "Denver", "80201", "USA"),
            )
        val company = mapCompanyDtoToCompany(companyDto)

        assertNotNull(company)
        assertEquals("company-3", company?.id)
        assertEquals("Solo Corp", company?.name)
        assertNotNull(company?.employees)
        assertTrue(company?.employees?.isEmpty() == true) // Should default to emptyList()
    }
}
