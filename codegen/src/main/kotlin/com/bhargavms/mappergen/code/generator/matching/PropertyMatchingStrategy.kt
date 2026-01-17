package com.bhargavms.mappergen.code.generator.matching

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Strategy interface for matching source properties to target properties.
 * Implementations can provide different matching algorithms (exact, normalized, fuzzy).
 */
interface PropertyMatchingStrategy {
    /**
     * Find a matching property in [sourceClass] for the given [targetPropertyName].
     *
     * @param sourceClass The source class containing candidate properties
     * @param targetPropertyName The name of the target property to match
     * @return The matching source property, or null if no match found
     */
    fun findMatchingProperty(
        sourceClass: KSClassDeclaration,
        targetPropertyName: String,
    ): KSPropertyDeclaration?
}

/**
 * Exact case-insensitive matching (the original behavior).
 * Matches "userName" to "username", "UserName", etc.
 */
object ExactCaseInsensitiveStrategy : PropertyMatchingStrategy {
    override fun findMatchingProperty(
        sourceClass: KSClassDeclaration,
        targetPropertyName: String,
    ): KSPropertyDeclaration? =
        sourceClass.getAllProperties().firstOrNull {
            it.simpleName.asString().equals(targetPropertyName, ignoreCase = true)
        }
}

/**
 * Normalized name matching strategy.
 * Normalizes property names by stripping common prefixes/suffixes and converting to a canonical form.
 * This handles common naming convention differences like:
 * - "userId" <-> "user_id" <-> "UserId"
 * - "isActive" <-> "active"
 * - "getName" <-> "name"
 */
object NormalizedNameStrategy : PropertyMatchingStrategy {
    override fun findMatchingProperty(
        sourceClass: KSClassDeclaration,
        targetPropertyName: String,
    ): KSPropertyDeclaration? {
        val normalizedTarget = normalize(targetPropertyName)
        return sourceClass.getAllProperties().firstOrNull {
            normalize(it.simpleName.asString()) == normalizedTarget
        }
    }

    /**
     * Normalize a property name to a canonical form for comparison.
     * - Converts to lowercase
     * - Removes underscores (snake_case -> snakecase)
     * - Strips common prefixes: "get", "set", "is", "has", "m_", "_"
     * - Strips common suffixes: "_"
     */
    internal fun normalize(name: String): String {
        var result =
            name
                .lowercase()
                .replace("_", "")

        // Strip common getter/setter/boolean prefixes
        val prefixes = listOf("get", "set", "is", "has", "m")
        for (prefix in prefixes) {
            if (result.startsWith(prefix) && result.length > prefix.length) {
                val afterPrefix = result.substring(prefix.length)
                // Only strip if the character after prefix was originally uppercase
                // (indicates it's a prefix, not part of the word)
                if (name.length > prefix.length && name[prefix.length].isUpperCase()) {
                    result = afterPrefix
                    break
                }
            }
        }

        return result
    }
}

/**
 * Fuzzy matching using Levenshtein distance.
 * Allows for minor spelling differences or typos.
 *
 * @param maxDistance The maximum edit distance allowed for a match (default: 2)
 * @param fallback An optional fallback strategy to try first for exact matches
 */
class FuzzyMatchingStrategy(
    private val maxDistance: Int = 2,
    private val fallback: PropertyMatchingStrategy = NormalizedNameStrategy,
) : PropertyMatchingStrategy {
    override fun findMatchingProperty(
        sourceClass: KSClassDeclaration,
        targetPropertyName: String,
    ): KSPropertyDeclaration? {
        // First try the fallback strategy for exact/normalized matches
        fallback.findMatchingProperty(sourceClass, targetPropertyName)?.let { return it }

        // Then try fuzzy matching
        val normalizedTarget = NormalizedNameStrategy.normalize(targetPropertyName)
        val candidates =
            sourceClass
                .getAllProperties()
                .map { it to levenshteinDistance(NormalizedNameStrategy.normalize(it.simpleName.asString()), normalizedTarget) }
                .filter { it.second <= maxDistance }
                .sortedBy { it.second }
                .toList()

        return candidates.firstOrNull()?.first
    }

    companion object {
        /**
         * Calculate the Levenshtein edit distance between two strings.
         * This is the minimum number of single-character edits (insertions, deletions, substitutions)
         * required to change one string into the other.
         */
        internal fun levenshteinDistance(
            s1: String,
            s2: String,
        ): Int {
            if (s1 == s2) return 0
            if (s1.isEmpty()) return s2.length
            if (s2.isEmpty()) return s1.length

            val len1 = s1.length
            val len2 = s2.length

            // Use two rows instead of full matrix for space efficiency
            var prevRow = IntArray(len2 + 1) { it }
            var currRow = IntArray(len2 + 1)

            for (i in 1..len1) {
                currRow[0] = i
                for (j in 1..len2) {
                    val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                    currRow[j] =
                        minOf(
                            prevRow[j] + 1, // deletion
                            currRow[j - 1] + 1, // insertion
                            prevRow[j - 1] + cost, // substitution
                        )
                }
                // Swap rows
                val temp = prevRow
                prevRow = currRow
                currRow = temp
            }

            return prevRow[len2]
        }
    }
}

/**
 * Composite strategy that tries multiple strategies in order.
 * Returns the first match found.
 */
class CompositeMatchingStrategy(
    private val strategies: List<PropertyMatchingStrategy>,
) : PropertyMatchingStrategy {
    override fun findMatchingProperty(
        sourceClass: KSClassDeclaration,
        targetPropertyName: String,
    ): KSPropertyDeclaration? {
        for (strategy in strategies) {
            strategy.findMatchingProperty(sourceClass, targetPropertyName)?.let { return it }
        }
        return null
    }
}

/**
 * Available matching strategy types that can be specified via annotation.
 */
enum class MatchingStrategyType {
    /** Exact case-insensitive matching (original behavior) */
    EXACT,

    /** Normalized name matching (handles naming convention differences) */
    NORMALIZED,

    /** Fuzzy matching with Levenshtein distance */
    FUZZY,
    ;

    fun toStrategy(): PropertyMatchingStrategy =
        when (this) {
            EXACT -> ExactCaseInsensitiveStrategy
            NORMALIZED -> NormalizedNameStrategy
            FUZZY -> FuzzyMatchingStrategy()
        }
}
