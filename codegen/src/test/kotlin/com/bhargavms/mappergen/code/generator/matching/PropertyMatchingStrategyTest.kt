package com.bhargavms.mappergen.code.generator.matching

import com.bhargavms.mappergen.testsupport.MinimalKspStubs
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PropertyMatchingStrategyTest {
    @Nested
    inner class NormalizedNameStrategyTest {
        @Test
        fun `normalize removes underscores`() {
            assertEquals("username", NormalizedNameStrategy.normalize("user_name"))
            assertEquals("userid", NormalizedNameStrategy.normalize("user_id"))
        }

        @Test
        fun `normalize converts to lowercase`() {
            assertEquals("username", NormalizedNameStrategy.normalize("UserName"))
            assertEquals("username", NormalizedNameStrategy.normalize("USERNAME"))
        }

        @Test
        fun `normalize handles snake_case to camelCase conversion`() {
            assertEquals("emailaddress", NormalizedNameStrategy.normalize("email_address"))
            assertEquals("emailaddress", NormalizedNameStrategy.normalize("emailAddress"))
            assertEquals("emailaddress", NormalizedNameStrategy.normalize("EmailAddress"))
        }

        @Test
        fun `normalize strips get prefix when followed by uppercase`() {
            assertEquals("name", NormalizedNameStrategy.normalize("getName"))
            assertEquals("getname", NormalizedNameStrategy.normalize("getname"))
        }

        @Test
        fun `normalize strips is prefix when followed by uppercase`() {
            assertEquals("active", NormalizedNameStrategy.normalize("isActive"))
            assertEquals("isactive", NormalizedNameStrategy.normalize("isactive"))
        }

        @Test
        fun `normalize strips has prefix when followed by uppercase`() {
            assertEquals("value", NormalizedNameStrategy.normalize("hasValue"))
            assertEquals("hasvalue", NormalizedNameStrategy.normalize("hasvalue"))
        }

        @Test
        fun `normalize strips set prefix when followed by uppercase`() {
            assertEquals("value", NormalizedNameStrategy.normalize("setValue"))
        }

        @Test
        fun `normalize strips is prefix when followed by underscore`() {
            assertEquals("active", NormalizedNameStrategy.normalize("is_active"))
            assertEquals("enabled", NormalizedNameStrategy.normalize("is_enabled"))
        }

        @Test
        fun `normalize strips get prefix when followed by underscore`() {
            assertEquals("name", NormalizedNameStrategy.normalize("get_name"))
            assertEquals("value", NormalizedNameStrategy.normalize("get_value"))
        }

        @Test
        fun `normalize strips has prefix when followed by underscore`() {
            assertEquals("items", NormalizedNameStrategy.normalize("has_items"))
        }

        @Test
        fun `normalize strips set prefix when followed by underscore`() {
            assertEquals("value", NormalizedNameStrategy.normalize("set_value"))
        }

        @Test
        fun `normalize handles prefix-only names without crashing`() {
            assertEquals("get", NormalizedNameStrategy.normalize("get"))
            assertEquals("is", NormalizedNameStrategy.normalize("is"))
            assertEquals("has", NormalizedNameStrategy.normalize("has"))
            assertEquals("set", NormalizedNameStrategy.normalize("set"))
        }

        @Test
        fun `normalize handles empty and single character names`() {
            assertEquals("", NormalizedNameStrategy.normalize(""))
            assertEquals("a", NormalizedNameStrategy.normalize("a"))
        }

        @Test
        fun `normalize handles trailing and leading underscores`() {
            assertEquals("name", NormalizedNameStrategy.normalize("_name_"))
            assertEquals("value", NormalizedNameStrategy.normalize("value_"))
        }

        @Test
        fun `normalize does not strip getter prefix when not followed by uppercase or underscore`() {
            assertEquals("getter", NormalizedNameStrategy.normalize("getter"))
        }

        @Test
        fun `normalize exercises each configured prefix branch`() {
            assertEquals("name", NormalizedNameStrategy.normalize("getName"))
            assertEquals("value", NormalizedNameStrategy.normalize("setValue"))
            assertEquals("active", NormalizedNameStrategy.normalize("isActive"))
            assertEquals("items", NormalizedNameStrategy.normalize("hasItems"))
            assertEquals("name", NormalizedNameStrategy.normalize("get_name"))
            assertEquals("value", NormalizedNameStrategy.normalize("set_value"))
            assertEquals("active", NormalizedNameStrategy.normalize("is_active"))
            assertEquals("items", NormalizedNameStrategy.normalize("has_items"))
        }

        @Test
        fun `normalize handles m prefix when followed by uppercase`() {
            assertEquals("value", NormalizedNameStrategy.normalize("mValue"))
        }

        @Test
        fun `normalize should not strip m prefix solely because next char is underscore`() {
            assertEquals("mvalue", NormalizedNameStrategy.normalize("m_value"))
        }
    }

    @Nested
    inner class FuzzyMatchingStrategyTest {
        @Test
        fun `levenshtein distance above max is rejected`() {
            assertEquals(3, FuzzyMatchingStrategy.levenshteinDistance("abc", "xyz"))
        }

        @Test
        fun `levenshtein distance for identical strings is 0`() {
            assertEquals(0, FuzzyMatchingStrategy.levenshteinDistance("hello", "hello"))
            assertEquals(0, FuzzyMatchingStrategy.levenshteinDistance("", ""))
        }

        @Test
        fun `levenshtein distance for empty string equals other string length`() {
            assertEquals(5, FuzzyMatchingStrategy.levenshteinDistance("", "hello"))
            assertEquals(5, FuzzyMatchingStrategy.levenshteinDistance("hello", ""))
        }

        @Test
        fun `levenshtein distance for single character difference is 1`() {
            assertEquals(1, FuzzyMatchingStrategy.levenshteinDistance("hello", "hallo"))
            assertEquals(1, FuzzyMatchingStrategy.levenshteinDistance("cat", "bat"))
        }

        @Test
        fun `levenshtein distance for insertion is 1`() {
            assertEquals(1, FuzzyMatchingStrategy.levenshteinDistance("hello", "helllo"))
        }

        @Test
        fun `levenshtein distance for deletion is 1`() {
            assertEquals(1, FuzzyMatchingStrategy.levenshteinDistance("hello", "helo"))
        }

        @Test
        fun `levenshtein distance for completely different strings`() {
            assertEquals(4, FuzzyMatchingStrategy.levenshteinDistance("abcd", "wxyz"))
        }

        @Test
        fun `levenshtein distance for typical typos`() {
            assertEquals(1, FuzzyMatchingStrategy.levenshteinDistance("username", "usrname"))
            assertEquals(2, FuzzyMatchingStrategy.levenshteinDistance("email", "emial"))
        }

        @Test
        fun `levenshtein distance handles unicode characters`() {
            assertEquals(1, FuzzyMatchingStrategy.levenshteinDistance("café", "cafe"))
        }

        @Test
        fun `levenshtein distance for transposition is 2`() {
            assertEquals(2, FuzzyMatchingStrategy.levenshteinDistance("ab", "ba"))
        }
    }

    @Nested
    inner class CompositeMatchingStrategyTest {
        @Test
        fun `empty strategy list returns null`() {
            assertNull(
                CompositeMatchingStrategy(emptyList())
                    .findMatchingProperty(MinimalKspStubs.unusedClassDeclaration, "name"),
            )
        }

        @Test
        fun `first matching strategy wins`() {
            val matchedProperty = MinimalKspStubs.unusedPropertyDeclaration
            val first =
                object : PropertyMatchingStrategy {
                    override fun findMatchingProperty(
                        sourceClass: KSClassDeclaration,
                        targetPropertyName: String,
                    ): KSPropertyDeclaration? = matchedProperty
                }
            val second =
                object : PropertyMatchingStrategy {
                    override fun findMatchingProperty(
                        sourceClass: KSClassDeclaration,
                        targetPropertyName: String,
                    ): KSPropertyDeclaration? = error("second strategy should not run")
                }
            val composite = CompositeMatchingStrategy(listOf(first, second))
            assertSame(matchedProperty, composite.findMatchingProperty(MinimalKspStubs.unusedClassDeclaration, "name"))
        }

        @Test
        fun `falls through to later strategy when earlier misses`() {
            val matchedProperty = MinimalKspStubs.unusedPropertyDeclaration
            val first =
                object : PropertyMatchingStrategy {
                    override fun findMatchingProperty(
                        sourceClass: KSClassDeclaration,
                        targetPropertyName: String,
                    ): KSPropertyDeclaration? = null
                }
            val second =
                object : PropertyMatchingStrategy {
                    override fun findMatchingProperty(
                        sourceClass: KSClassDeclaration,
                        targetPropertyName: String,
                    ): KSPropertyDeclaration? = matchedProperty
                }
            val composite = CompositeMatchingStrategy(listOf(first, second))
            assertSame(matchedProperty, composite.findMatchingProperty(MinimalKspStubs.unusedClassDeclaration, "name"))
        }

        @Test
        fun `returns null when all strategies miss`() {
            val composite =
                CompositeMatchingStrategy(
                    listOf(
                        object : PropertyMatchingStrategy {
                            override fun findMatchingProperty(
                                sourceClass: KSClassDeclaration,
                                targetPropertyName: String,
                            ): KSPropertyDeclaration? = null
                        },
                        object : PropertyMatchingStrategy {
                            override fun findMatchingProperty(
                                sourceClass: KSClassDeclaration,
                                targetPropertyName: String,
                            ): KSPropertyDeclaration? = null
                        },
                    ),
                )
            assertNull(composite.findMatchingProperty(MinimalKspStubs.unusedClassDeclaration, "name"))
        }
    }

    @Nested
    inner class MatchingStrategyTypeTest {
        @Test
        fun `toStrategy returns correct strategy type`() {
            assertTrue(MatchingStrategyType.EXACT.toStrategy() is ExactCaseInsensitiveStrategy)
            assertTrue(MatchingStrategyType.NORMALIZED.toStrategy() is NormalizedNameStrategy)
            assertTrue(MatchingStrategyType.FUZZY.toStrategy() is FuzzyMatchingStrategy)
        }
    }
}
