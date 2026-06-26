package com.example

public fun mapAddressDtoToAddress(input: Network.AddressDto?): Domain.Address? =
    input?.let {
        Domain.Address(
            street = it.street ?: "",
            city = it.city ?: "",
            zipCode = it.zipCode ?: "",
            country = it.country ?: "",
        )
    }
