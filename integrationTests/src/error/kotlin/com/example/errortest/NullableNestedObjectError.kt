package com.example.errortest

import com.bhargavms.mappergen.annotations.Mapper

// This should fail: nullable nested object -> non-nullable
data class SourceDto(
    val address: AddressDto?,
)

data class AddressDto(
    val street: String,
    val city: String,
)

data class Target(
    val address: Address,
)

data class Address(
    val street: String,
    val city: String,
)

interface TestMapper {
    @Mapper
    fun map(dto: SourceDto): Target
}
