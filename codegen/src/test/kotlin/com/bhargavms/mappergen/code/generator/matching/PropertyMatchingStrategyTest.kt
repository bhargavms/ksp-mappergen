package com.bhargavms.mappergen.code.generator.matching

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
            assertEquals("getname", NormalizedNameStrategy.normalize("getname")) // no uppercase after get
        }

        @Test
        fun `normalize strips is prefix when followed by uppercase`() {
            assertEquals("active", NormalizedNameStrategy.normalize("isActive"))
            assertEquals("isactive", NormalizedNameStrategy.normalize("isactive")) // no uppercase after is
        }

        @Test
        fun `normalize strips has prefix when followed by uppercase`() {
            assertEquals("value", NormalizedNameStrategy.normalize("hasValue"))
            assertEquals("hasvalue", NormalizedNameStrategy.normalize("hasvalue")) // no uppercase after has
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
            // Edge case: name equals prefix exactly
            assertEquals("get", NormalizedNameStrategy.normalize("get"))
            assertEquals("is", NormalizedNameStrategy.normalize("is"))
            assertEquals("has", NormalizedNameStrategy.normalize("has"))
            assertEquals("set", NormalizedNameStrategy.normalize("set"))
        }
    }

    @Nested
    inner class FuzzyMatchingStrategyTest {
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
            // "username" -> "usrname" is 1 deletion (remove 'e')
            assertEquals(1, FuzzyMatchingStrategy.levenshteinDistance("username", "usrname"))
            // "email" -> "emial" is 2 operations (swap 'i' and 'a')
            assertEquals(2, FuzzyMatchingStrategy.levenshteinDistance("email", "emial"))
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
