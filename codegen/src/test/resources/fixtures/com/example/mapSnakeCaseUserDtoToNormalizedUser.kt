package com.example

public fun mapSnakeCaseUserDtoToNormalizedUser(input: Network.SnakeCaseUserDto?): Domain.NormalizedUser? =
    input?.let {
        Domain.NormalizedUser(
            userId = it.user_id ?: "",
            userName = it.user_name ?: "",
            emailAddress = it.email_address ?: "",
            createdAt = it.created_at ?: 0L,
        )
    }
