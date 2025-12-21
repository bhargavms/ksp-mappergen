package com.example

public fun mapCompanyDtoToCompany(input: Network.CompanyDto?): Domain.Company? =
    input?.let {
        Domain.Company(
            id = it.id ?: "",
            name = it.name ?: "",
            employees = it.employees?.mapNotNull { mapPersonDtoToPerson(it) } ?: emptyList(),
            headquarters = mapAddressDtoToAddress(it.headquarters),
        )
    }
