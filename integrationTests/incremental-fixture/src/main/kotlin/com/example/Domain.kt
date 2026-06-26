package com.example

object Domain {
    data class User(
        val id: String,
        val name: String,
        val email: String,
        val age: Int,
    )
}
