package com.example

public fun mapPersonDtoToPerson(input: Network.PersonDto?): Domain.Person? =
    input?.let {
        Domain.Person(
            id = it.id ?: "",
            name = it.name ?: "",
            address = mapAddressDtoToAddress(it.address) ?: TODO("handle null"),
            age = it.age ?: 0,
        )
    }
