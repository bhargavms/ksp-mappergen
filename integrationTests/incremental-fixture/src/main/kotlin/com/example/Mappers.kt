package com.example

import com.bhargavms.mappergen.annotations.Mapper

interface Mappers {
    @Mapper
    fun mapUser(value: Network.UserDto): Domain.User
}
