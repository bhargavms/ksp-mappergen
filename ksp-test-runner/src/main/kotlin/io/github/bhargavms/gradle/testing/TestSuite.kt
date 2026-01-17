package io.github.bhargavms.gradle.testing

import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

/**
 * DSL for defining KSP integration test suites and tests.
 */
open class KspTestExtension(
    private val buildDir: Provider<Directory>,
) {
    private val suites = mutableListOf<TestSuite>()

    fun suite(
        name: String,
        block: TestSuite.() -> Unit,
    ) {
        val suite = TestSuite(name = name, parentPath = name)
        suite.block()
        suites.add(suite)
    }

    internal fun hasFocusedTests(): Boolean = suites.any { it.hasFocusedTests() }

    internal fun run(
        filter: Regex? = null,
        reporter: TestReporter = TestReporter(),
    ): TestReporter {
        val hasFocus = hasFocusedTests()
        suites.forEach { suite ->
            suite.run(filter, reporter, hasFocus)
        }

        reporter.printFailures()
        reporter.printSummary()

        if (!reporter.allTestsPassed()) {
            throw GradleException("Test failures in KSP test runner")
        }

        return reporter
    }
}

class TestSuite internal constructor(
    private val name: String,
    private val parentPath: String,
) {
    private val suites = mutableListOf<TestSuite>()
    private val tests = mutableListOf<TestCase>()
    private val beforeAllHooks = mutableListOf<() -> Unit>()
    private val afterAllHooks = mutableListOf<() -> Unit>()
    private val beforeEachHooks = mutableListOf<() -> Unit>()
    private val afterEachHooks = mutableListOf<() -> Unit>()

    fun suite(
        name: String,
        block: TestSuite.() -> Unit,
    ) {
        val fullName = "$parentPath > $name"
        val child = TestSuite(name, fullName)
        child.block()
        suites.add(child)
    }

    fun beforeAll(block: () -> Unit) {
        beforeAllHooks.add(block)
    }

    fun afterAll(block: () -> Unit) {
        afterAllHooks.add(block)
    }

    fun beforeEach(block: () -> Unit) {
        beforeEachHooks.add(block)
    }

    fun afterEach(block: () -> Unit) {
        afterEachHooks.add(block)
    }

    fun test(
        name: String,
        block: () -> Unit,
    ) {
        tests.add(TestCase(name = name, block = block))
    }

    fun ftest(
        name: String,
        block: () -> Unit,
    ) {
        tests.add(TestCase(name = name, block = block, focused = true))
    }

    fun xtest(
        name: String,
        reason: String = "skipped",
        block: () -> Unit = {},
    ) {
        tests.add(TestCase(name = name, block = block, skippedReason = reason))
    }

    internal fun hasFocusedTests(): Boolean {
        val selfFocus = tests.any { it.focused }
        val childrenFocus = suites.any { it.hasFocusedTests() }
        return selfFocus || childrenFocus
    }

    internal fun run(
        filter: Regex?,
        reporter: TestReporter,
        hasGlobalFocus: Boolean,
    ) {
        fun shouldSkip(
            test: TestCase,
            path: String,
        ): Boolean {
            if (test.skippedReason != null) return true
            if (hasGlobalFocus && !test.focused) return true
            if (filter != null && !filter.containsMatchIn(path)) return true
            return false
        }

        beforeAllHooks.forEach { hook -> hook() }

        tests.forEach { test ->
            val path = "$parentPath > ${test.name}"
            val skip = shouldSkip(test, path)
            if (!skip) {
                reporter.testStarted(parentPath, name, test.name)
            }
            var duration = 0L
            var error: Throwable? = null

            if (!skip) {
                val start = System.currentTimeMillis()
                try {
                    beforeEachHooks.forEach { it() }
                    test.block()
                    duration = System.currentTimeMillis() - start
                } catch (t: Throwable) {
                    error = t
                    duration = System.currentTimeMillis() - start
                } finally {
                    try {
                        afterEachHooks.forEach { it() }
                    } catch (hookError: Throwable) {
                        if (error == null) error = hookError
                    }
                }
            }

            if (skip) {
                reporter.testFinished(
                    TestReporter.TestResult(
                        suitePath = parentPath,
                        suiteName = name,
                        testName = test.name,
                        status = TestReporter.Status.SKIPPED,
                        durationMs = duration,
                        skippedReason = test.skippedReason ?: "filtered",
                    ),
                )
            } else if (error == null) {
                reporter.testFinished(
                    TestReporter.TestResult(
                        suitePath = parentPath,
                        suiteName = name,
                        testName = test.name,
                        status = TestReporter.Status.PASSED,
                        durationMs = duration,
                    ),
                )
            } else {
                reporter.testFinished(
                    TestReporter.TestResult(
                        suitePath = parentPath,
                        suiteName = name,
                        testName = test.name,
                        status = TestReporter.Status.FAILED,
                        durationMs = duration,
                        error = error,
                    ),
                )
            }
        }

        suites.forEach { child ->
            child.run(filter, reporter, hasGlobalFocus)
        }

        afterAllHooks.forEach { hook -> hook() }
    }
}

private data class TestCase(
    val name: String,
    val block: () -> Unit,
    val skippedReason: String? = null,
    val focused: Boolean = false,
)

/**
 * Entry point to run KSP tests from build scripts.
 */
fun runKspTests(
    extension: KspTestExtension,
    filter: Regex? = null,
): TestReporter = extension.run(filter = filter)
