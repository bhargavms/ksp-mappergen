package com.example

import com.bhargavms.mappergen.annotations.Mapper
import com.bhargavms.mappergen.annotations.MatchingStrategy
import com.bhargavms.mappergen.annotations.PropertyTransform

/**
 * Define your mapper functions here.
 * The @Mapper annotation triggers code generation.
 */
interface Mappers {
    // Basic mappings
    @Mapper
    fun mapUser(value: Network.UserDto): Domain.User

    @Mapper
    fun mapProduct(value: Network.ProductDto): Domain.Product

    // All primitive types
    @Mapper
    fun mapPrimitives(value: Network.PrimitivesDto): Domain.Primitives

    // Nested objects
    @Mapper
    fun mapAddress(value: Network.AddressDto): Domain.Address

    @Mapper
    fun mapPerson(value: Network.PersonDto): Domain.Person

    // Collections (simple types)
    @Mapper
    fun mapOrder(value: Network.OrderDto): Domain.Order

    // Enums - now supported!
    @Mapper
    fun mapAccount(value: Network.AccountDto): Domain.Account

    // Complex nested structures - now supported!
    @Mapper
    fun mapCompany(value: Network.CompanyDto): Domain.Company

    // Test normalized matching: snake_case to camelCase
    @Mapper(matchingStrategy = MatchingStrategy.NORMALIZED)
    fun mapNormalizedUser(value: Network.SnakeCaseUserDto): Domain.NormalizedUser

    // Test custom transformations with inline expressions
    @Mapper(
        transforms = [
            PropertyTransform(target = "fullName", expression = "(it.firstName.orEmpty()) + \" \" + (it.lastName.orEmpty())"),
            PropertyTransform(target = "age", expression = "java.time.Year.now().value - (it.birthYear ?: 2000)"),
            PropertyTransform(
                target = "fullAddress",
                expression = "listOfNotNull(it.addressLine1, it.addressLine2, it.cityName).joinToString()",
            ),
        ],
    )
    fun mapCustomer(value: Network.CustomerDto): Domain.Customer
}
