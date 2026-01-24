package com.example

public fun mapCustomerDtoToCustomer(input: Network.CustomerDto?): Domain.Customer? = input?.let {
    Domain.Customer(
        fullName = (it.firstName.orEmpty()) + " " + (it.lastName.orEmpty()),
        age = java.time.Year.now().value - (it.birthYear ?: 2000),
        fullAddress = listOfNotNull(it.addressLine1, it.addressLine2, it.cityName).joinToString()
    )
}

