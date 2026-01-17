package com.bhargavms.mappergen.annotations

/**
 * Available property matching strategies for mappers.
 */
enum class MatchingStrategy {
    /** Exact case-insensitive matching (default behavior) */
    EXACT,

    /** Normalized name matching - handles naming convention differences (snake_case, camelCase, etc.) */
    NORMALIZED,

    /** Fuzzy matching using Levenshtein distance - allows for minor spelling differences */
    FUZZY,
}

/**
 * Defines a custom property transformation for a specific target property.
 *
 * Use this to:
 * - Map from a differently-named source property
 * - Apply a custom transformation expression
 *
 * Example:
 * ```kotlin
 * @Mapper(
 *     transforms = [
 *         PropertyTransform(
 *             target = "fullName",
 *             expression = "it.firstName + \" \" + it.lastName"
 *         ),
 *         PropertyTransform(
 *             target = "userId",
 *             source = "id"  // Map from 'id' property to 'userId'
 *         )
 *     ]
 * )
 * fun map(dto: UserDto): User
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target() // Only used as nested annotation
annotation class PropertyTransform(
    /**
     * The name of the target property to transform.
     * or more precisely
     * [target] should be the property name in the output class you're mapping to
     */
    val target: String,
    /**
     * The name of the source property to map from.
     * If not specified, defaults to the target property name.
     */
    val source: String = "",
    /**
     * A Kotlin expression to compute the target value.
     * The input object is available as 'it'.
     *
     * Examples:
     * - "it.firstName + \" \" + it.lastName"
     * - "it.timestamp.toEpochMilli()"
     * - "it.items.size"
     */
    val expression: String = "",
)

/**
 * Marks a function as a mapper that should have an implementation generated.
 *
 * The annotated function should be declared in an interface with:
 * - Exactly one parameter (the source type)
 * - A return type (the target type)
 *
 * MapperGen will generate a mapping function that:
 * - Matches properties by name using the specified [matchingStrategy]
 * - Applies any custom [transforms] for specific properties
 * - Handles nullable to non-nullable conversions with sensible defaults
 *
 * Example:
 * ```kotlin
 * interface UserMappers {
 *     @Mapper(
 *         matchingStrategy = MatchingStrategy.NORMALIZED,
 *         transforms = [
 *             PropertyTransform(target = "fullName", expression = "it.firstName + \" \" + it.lastName")
 *         ]
 *     )
 *     fun map(dto: UserDto): User
 * }
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Mapper(
    /**
     * The strategy to use for matching source properties to target properties.
     * Default is [MatchingStrategy.EXACT] for backward compatibility.
     */
    val matchingStrategy: MatchingStrategy = MatchingStrategy.EXACT,
    /**
     * Custom property transformations to apply.
     * Each [PropertyTransform] specifies how to handle a specific target property.
     */
    val transforms: Array<PropertyTransform> = [],
)
