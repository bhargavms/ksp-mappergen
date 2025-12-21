package com.example

/**
 * Network layer DTOs - nullable types from API responses
 */
object Network {
    data class UserDto(
        val id: String?,
        val name: String?,
        val email: String?,
        val age: Int?,
    )

    data class ProductDto(
        val id: String?,
        val title: String?,
        val price: Double?,
    )

    // Test all primitive types (nullable)
    data class PrimitivesDto(
        val byteValue: Byte?,
        val shortValue: Short?,
        val intValue: Int?,
        val longValue: Long?,
        val floatValue: Float?,
        val doubleValue: Double?,
        val booleanValue: Boolean?,
        val charValue: Char?,
        val stringValue: String?,
    )

    // Nested objects
    data class AddressDto(
        val street: String?,
        val city: String?,
        val zipCode: String?,
        val country: String?,
    )

    data class PersonDto(
        val id: String?,
        val name: String?,
        val address: AddressDto?,
        val age: Int?,
    )

    // Collections
    data class OrderDto(
        val id: String?,
        val items: List<String>?,
        val quantities: List<Int>?,
        val total: Double?,
    )

    // Enums
    enum class StatusDto {
        ACTIVE,
        INACTIVE,
        PENDING,
    }

    data class AccountDto(
        val id: String?,
        val status: StatusDto?,
        val balance: Double?,
    )

    // Complex nested structure
    data class CompanyDto(
        val id: String?,
        val name: String?,
        val employees: List<PersonDto>?,
        val headquarters: AddressDto?,
    )
}
