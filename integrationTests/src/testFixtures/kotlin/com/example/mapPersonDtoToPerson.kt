package com.example

public fun mapPersonDtoToPerson(input: Network.PersonDto?): Domain.Person? = input?.let {
    Domain.Person(
        id = it.id ?: "",
        name = it.name ?: "",
        address = mapAddressDtoToAddress(it.address),
        age = it.age ?: 0
    )
}
