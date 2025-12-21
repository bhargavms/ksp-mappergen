package io.github.bhargavms.gradle.testing

import java.io.File

/**
 * JUnit-like assertion helpers for the KSP test runner.
 * Assertions throw [AssertionError] to integrate with Gradle task failures.
 */
object Assertions {
    /** Asserts that two values are equal. */
    fun <T> assertEquals(
        expected: T,
        actual: T,
        message: String? = null,
    ) {
        if (expected != actual) {
            val msg = message ?: "Expected <$expected>, but was <$actual>"
            throw AssertionError(msg)
        }
    }

    /** Asserts that two values are not equal. */
    fun <T> assertNotEquals(
        unexpected: T,
        actual: T,
        message: String? = null,
    ) {
        if (unexpected == actual) {
            val msg = message ?: "Expected not equal to <$unexpected>, but was <$actual>"
            throw AssertionError(msg)
        }
    }

    /** Asserts that a condition is true. */
    fun assertTrue(
        condition: Boolean,
        message: String? = null,
    ) {
        if (!condition) {
            val msg = message ?: "Expected condition to be true"
            throw AssertionError(msg)
        }
    }

    /** Asserts that a condition is false. */
    fun assertFalse(
        condition: Boolean,
        message: String? = null,
    ) {
        if (condition) {
            val msg = message ?: "Expected condition to be false"
            throw AssertionError(msg)
        }
    }

    /** Asserts that a value is null. */
    fun <T> assertNull(
        actual: T?,
        message: String? = null,
    ) {
        if (actual != null) {
            val msg = message ?: "Expected null, but was <$actual>"
            throw AssertionError(msg)
        }
    }

    /** Asserts that a value is not null and returns it. */
    fun <T> assertNotNull(
        actual: T?,
        message: String? = null,
    ): T {
        if (actual == null) {
            val msg = message ?: "Expected not null"
            throw AssertionError(msg)
        }
        return actual
    }

    /** Asserts that a file exists. */
    fun assertFileExists(
        file: File,
        message: String? = null,
    ) {
        if (!file.exists()) {
            val msg = message ?: "Expected file to exist: ${file.absolutePath}"
            throw AssertionError(msg)
        }
    }

    /** Asserts that a file does not exist. */
    fun assertFileNotExists(
        file: File,
        message: String? = null,
    ) {
        if (file.exists()) {
            val msg = message ?: "Expected file to not exist: ${file.absolutePath}"
            throw AssertionError(msg)
        }
    }

    /** Asserts that a string contains a substring. */
    fun assertContains(
        content: String,
        substring: String,
        message: String? = null,
    ) {
        if (!content.contains(substring)) {
            val msg = message ?: "Expected content to contain '$substring'"
            throw AssertionError(msg)
        }
    }

    /** Asserts that a string does not contain a substring. */
    fun assertNotContains(
        content: String,
        substring: String,
        message: String? = null,
    ) {
        if (content.contains(substring)) {
            val msg = message ?: "Expected content to not contain '$substring'"
            throw AssertionError(msg)
        }
    }

    /** Asserts that a string matches a regular expression. */
    fun assertMatches(
        content: String,
        regex: Regex,
        message: String? = null,
    ) {
        if (!regex.containsMatchIn(content)) {
            val msg = message ?: "Expected content to match pattern: ${regex.pattern}"
            throw AssertionError(msg)
        }
    }

    /** Asserts that a string does not match a regular expression. */
    fun assertNotMatches(
        content: String,
        regex: Regex,
        message: String? = null,
    ) {
        if (regex.containsMatchIn(content)) {
            val msg = message ?: "Expected content to not match pattern: ${regex.pattern}"
            throw AssertionError(msg)
        }
    }

    /** Asserts that a block throws a specific type of exception. */
    inline fun <reified T : Throwable> assertThrows(
        message: String? = null,
        block: () -> Unit,
    ) {
        try {
            block()
            val msg = message ?: "Expected ${T::class.simpleName} to be thrown"
            throw AssertionError(msg)
        } catch (e: Throwable) {
            if (e !is T) {
                val msg = message ?: "Expected ${T::class.simpleName}, but ${e::class.simpleName} was thrown"
                throw AssertionError(msg)
            }
        }
    }

    /** Asserts that a block does not throw any exception. */
    fun assertDoesNotThrow(
        message: String? = null,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (e: Throwable) {
            val msg = message ?: "Expected no exception, but ${e::class.simpleName} was thrown: ${e.message}"
            throw AssertionError(msg)
        }
    }

    /** Asserts that two text contents are equal (trimmed). */
    fun assertContentEquals(
        expected: String,
        actual: String,
        message: String? = null,
    ) {
        val expectedTrimmed = expected.trim()
        val actualTrimmed = actual.trim()
        if (expectedTrimmed != actualTrimmed) {
            val msg = message ?: "Content mismatch:\\nExpected:\\n$expectedTrimmed\\n\\nActual:\\n$actualTrimmed"
            throw AssertionError(msg)
        }
    }

    /** Asserts that file content matches expected content. */
    fun assertFileContentEquals(
        expectedFile: File,
        actualFile: File,
        message: String? = null,
    ) {
        assertFileExists(expectedFile, "Expected file does not exist: ${expectedFile.absolutePath}")
        assertFileExists(actualFile, "Actual file does not exist: ${actualFile.absolutePath}")

        val expectedContent = expectedFile.readText()
        val actualContent = actualFile.readText()

        assertContentEquals(
            expectedContent,
            actualContent,
            message ?: "File content mismatch: ${expectedFile.name} vs ${actualFile.name}",
        )
    }
}
