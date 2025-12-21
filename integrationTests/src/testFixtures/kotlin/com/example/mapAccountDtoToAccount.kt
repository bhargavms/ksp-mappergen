package com.example

public fun mapAccountDtoToAccount(input: Network.AccountDto?): Domain.Account? =
    input?.let {
        Domain.Account(
            id = it.id ?: "",
            status =
                it.status?.let {
                    com.example.Domain.Status
                        .valueOf(it.name)
                }
                    ?: com.example.Domain.Status.PENDING,
            balance = it.balance ?: 0.0,
        )
    }
