package com.example

public fun mapProductDtoToProduct(input: Network.ProductDto?): Domain.Product? = input?.let {
    Domain.Product(
        id = it.id ?: "",
        title = it.title ?: "",
        price = it.price ?: 0.0
    )
}
