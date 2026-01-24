package com.example.sample

/**
 * Domain layer models - clean, non-nullable types
 */
object Domain {
    data class User(
        val id: String,
        val name: String,
        val email: String,
        val age: Int,
    )

    data class Product(
        val id: String,
        val title: String,
        val price: Double,
    )

    // Test all primitive types
    data class Primitives(
        val byteValue: Byte,
        val shortValue: Short,
        val intValue: Int,
        val longValue: Long,
        val floatValue: Float,
        val doubleValue: Double,
        val booleanValue: Boolean,
        val charValue: Char,
        val stringValue: String,
    )

    // Nested objects
    data class Address(
        val street: String,
        val city: String,
        val zipCode: String,
        val country: String,
    )

    data class Person(
        val id: String,
        val name: String,
        val address: Address?,
        val age: Int,
    )

    // Collections
    data class Order(
        val id: String,
        val items: List<String>,
        val quantities: List<Int>,
        val total: Double,
    )

    // Enums
    enum class Status {
        ACTIVE,
        INACTIVE,
        PENDING,
    }

    data class Account(
        val id: String,
        val status: Status,
        val balance: Double,
    )

    // Complex nested structure
    data class Company(
        val id: String,
        val name: String,
        val employees: List<Person>,
        val headquarters: Address?,
    )

    // For testing normalized matching (camelCase naming)
    data class NormalizedUser(
        val userId: String,
        val userName: String,
        val emailAddress: String,
        val createdAt: Long,
    )

    // For testing custom transformations
    data class Customer(
        val fullName: String,
        val age: Int,
        val fullAddress: String,
    )
}
