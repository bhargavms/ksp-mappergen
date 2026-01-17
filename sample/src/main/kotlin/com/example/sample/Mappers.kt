package com.example.sample

import com.bhargavms.mappergen.annotations.Mapper

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
}
