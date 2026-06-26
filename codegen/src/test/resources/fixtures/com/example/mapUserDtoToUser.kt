package com.example

public fun mapUserDtoToUser(input: Network.UserDto?): Domain.User? =
    input?.let {
        Domain.User(
            id = it.id ?: "",
            name = it.name ?: "",
            email = it.email ?: "",
            age = it.age ?: 0,
        )
    }
