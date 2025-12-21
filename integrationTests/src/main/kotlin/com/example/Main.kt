package com.example

/**
 * Comprehensive sample usage of generated mappers.
 * Generated mapper functions are in the same package, so no imports needed.
 * Run with: ./gradlew :sample:run
 */
fun main() {
    println("=== MapperGen Comprehensive Demo ===\n")

    // 1. Basic mappings
    testBasicMappings()

    // 2. All primitive types
    testPrimitives()

    // 3. Nested objects
    testNestedObjects()

    // 4. Collections
    testCollections()

    // 5. Enums (now fully supported!)
    testEnums()

    // 6. Complex nested structures (now fully supported!)
    testComplexStructures()

    // 7. Null handling
    testNullHandling()
}

fun testBasicMappings() {
    println("--- Basic Mappings ---")
    val userDto =
        Network.UserDto(
            id = "user-123",
            name = "John Doe",
            email = "john@example.com",
            age = 30,
        )
    val user = mapUserDtoToUser(userDto)
    println("User: $user")

    val productDto =
        Network.ProductDto(
            id = "prod-456",
            title = "Kotlin in Action",
            price = 39.99,
        )
    val product = mapProductDtoToProduct(productDto)
    println("Product: $product\n")
}

fun testPrimitives() {
    println("--- All Primitive Types ---")
    val primitivesDto =
        Network.PrimitivesDto(
            byteValue = 42,
            shortValue = 1000,
            intValue = 100000,
            longValue = 1000000000L,
            floatValue = 3.14f,
            doubleValue = 2.718,
            booleanValue = true,
            charValue = 'K',
            stringValue = "Hello Kotlin",
        )
    val primitives = mapPrimitivesDtoToPrimitives(primitivesDto)
    println("Primitives: $primitives")

    // Test with nulls (should use defaults)
    val partialPrimitives =
        Network.PrimitivesDto(
            byteValue = null,
            shortValue = null,
            intValue = null,
            longValue = null,
            floatValue = null,
            doubleValue = null,
            booleanValue = null,
            charValue = null,
            stringValue = null,
        )
    val defaults = mapPrimitivesDtoToPrimitives(partialPrimitives)
    println("With defaults: $defaults\n")
}

fun testNestedObjects() {
    println("--- Nested Objects ---")
    val addressDto =
        Network.AddressDto(
            street = "123 Main St",
            city = "San Francisco",
            zipCode = "94102",
            country = "USA",
        )
    val address = mapAddressDtoToAddress(addressDto)
    println("Address: $address")

    val personDto =
        Network.PersonDto(
            id = "person-1",
            name = "Alice",
            address = addressDto,
            age = 28,
        )
    val person = mapPersonDtoToPerson(personDto)
    println("Person: $person\n")
}

fun testCollections() {
    println("--- Collections (Simple Types) ---")
    val orderDto =
        Network.OrderDto(
            id = "order-789",
            items = listOf("Laptop", "Mouse", "Keyboard"),
            quantities = listOf(1, 2, 1),
            total = 1299.99,
        )
    val order = mapOrderDtoToOrder(orderDto)
    println("Order: $order")

    // Empty collections
    val emptyOrderDto =
        Network.OrderDto(
            id = "order-empty",
            items = emptyList(),
            quantities = emptyList(),
            total = 0.0,
        )
    val emptyOrder = mapOrderDtoToOrder(emptyOrderDto)
    println("Empty Order: $emptyOrder\n")
}

fun testEnums() {
    println("--- Enums ---")

    // Test enum mapping (StatusDto -> Status) via valueOf
    val activeAccountDto =
        Network.AccountDto(
            id = "account-1",
            status = Network.StatusDto.ACTIVE,
            balance = 1000.0,
        )
    val activeAccount = mapAccountDtoToAccount(activeAccountDto)
    println("Active Account: $activeAccount")

    val inactiveAccountDto =
        Network.AccountDto(
            id = "account-2",
            status = Network.StatusDto.INACTIVE,
            balance = 500.0,
        )
    val inactiveAccount = mapAccountDtoToAccount(inactiveAccountDto)
    println("Inactive Account: $inactiveAccount")

    // Null status - defaults to first enum value (ACTIVE)
    val nullStatusDto =
        Network.AccountDto(
            id = "account-3",
            status = null,
            balance = 0.0,
        )
    val nullStatusAccount = mapAccountDtoToAccount(nullStatusDto)
    println("Null status (defaults to ACTIVE): $nullStatusAccount\n")
}

fun testComplexStructures() {
    println("--- Complex Nested Structures (Fully Supported!) ---")
    val companyDto =
        Network.CompanyDto(
            id = "company-1",
            name = "Tech Corp",
            employees =
                listOf(
                    Network.PersonDto(
                        id = "emp-1",
                        name = "Bob",
                        address = Network.AddressDto("456 Oak Ave", "Seattle", "98101", "USA"),
                        age = 32,
                    ),
                    Network.PersonDto(
                        id = "emp-2",
                        name = "Carol",
                        address = Network.AddressDto("789 Pine St", "Portland", "97201", "USA"),
                        age = 29,
                    ),
                ),
            headquarters = Network.AddressDto("100 Tech Blvd", "San Jose", "95110", "USA"),
        )
    val company = mapCompanyDtoToCompany(companyDto)
    println("Company: $company")
    println("Company employees count: ${company?.employees?.size}")
    println("First employee: ${company?.employees?.first()}\n")
}

fun testNullHandling() {
    println("--- Null Handling ---")
    // Completely null input
    val nullUser = mapUserDtoToUser(null)
    println("Null input: $nullUser")

    // Partial nulls
    val partialUser =
        Network.UserDto(
            id = "user-789",
            name = null,
            email = null,
            age = null,
        )
    val mappedPartial = mapUserDtoToUser(partialUser)
    println("Partial nulls: $mappedPartial")

    println("Note: Null nested objects generate TODO in the mapper - handle manually if needed.\n")
}
