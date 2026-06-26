package com.example

public fun mapAccountDtoToAccount(input: Network.AccountDto?): Domain.Account? =
    input?.let {
        Domain.Account(
            id = it.id ?: "",
            status =
                when (it.status) {
                    null -> com.example.Domain.Status.ACTIVE
                    com.example.Network.StatusDto.ACTIVE -> com.example.Domain.Status.ACTIVE
                    com.example.Network.StatusDto.INACTIVE -> com.example.Domain.Status.INACTIVE
                    com.example.Network.StatusDto.PENDING -> com.example.Domain.Status.PENDING
                },
            balance = it.balance ?: 0.0,
        )
    }
