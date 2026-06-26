package com.example

fun main() {
    val user = mapUserDtoToUser(Network.UserDto("1", "Ada", "ada@example.com", 30))
    println(user?.name)
}
