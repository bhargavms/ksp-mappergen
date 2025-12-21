package com.example

public fun mapOrderDtoToOrder(input: Network.OrderDto?): Domain.Order? =
    input?.let {
        Domain.Order(
            id = it.id ?: "",
            items = it.items ?: emptyList(),
            quantities = it.quantities ?: emptyList(),
            total = it.total ?: 0.0,
        )
    }
