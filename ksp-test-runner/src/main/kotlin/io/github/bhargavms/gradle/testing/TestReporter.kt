package io.github.bhargavms.gradle.testing

/**
 * Collects and prints test results with colorized output and timing.
 */
class TestReporter {
    private val results = mutableListOf<TestResult>()
    private val startTime = System.currentTimeMillis()

    data class TestResult(
        val suitePath: String,
        val suiteName: String,
        val testName: String,
        val status: Status,
        val durationMs: Long,
        val error: Throwable? = null,
        val skippedReason: String? = null,
    )

    enum class Status { PASSED, FAILED, SKIPPED }

    fun testStarted(
        @Suppress("UNUSED_PARAMETER") suitePath: String,
        @Suppress("UNUSED_PARAMETER") suiteName: String,
        @Suppress("UNUSED_PARAMETER") testName: String,
    ) {
        print("$BLUE•$RESET")
    }

    fun testFinished(result: TestResult) {
        results.add(result)
        when (result.status) {
            Status.PASSED -> print("$GREEN✓$RESET")
            Status.FAILED -> print("$RED✗$RESET")
            Status.SKIPPED -> print("$YELLOW○$RESET")
        }
    }

    fun printFailures() {
        val failures = results.filter { it.status == Status.FAILED }
        if (failures.isEmpty()) return

        println("\n")
        println("${RED}${BOLD}Failures:${RESET}\n")

        failures.forEach { result ->
            println("$RED❌ ${result.suitePath} > ${result.testName}$RESET")
            println("   ${YELLOW}Duration: ${formatDuration(result.durationMs)}$RESET")
            result.error?.let { error ->
                println("   ${RED}Error: ${error.message}$RESET")
                val stack = error.stackTrace.take(10)
                if (stack.isNotEmpty()) {
                    println("   ${RED}Stack trace:$RESET")
                    stack.forEach { element -> println("     ${RED}at $element$RESET") }
                    if (error.stackTrace.size > stack.size) {
                        println("     $RED... (${error.stackTrace.size - stack.size} more)$RESET")
                    }
                }
            }
            println()
        }
    }

    fun printSummary() {
        val totalTime = System.currentTimeMillis() - startTime
        val passed = results.count { it.status == Status.PASSED }
        val failed = results.count { it.status == Status.FAILED }
        val skipped = results.count { it.status == Status.SKIPPED }
        val total = results.size

        println("\n${BOLD}Test Summary:$RESET")
        println("  ${GREEN}Passed: $passed$RESET")
        if (failed > 0) println("  ${RED}Failed: $failed$RESET")
        if (skipped > 0) println("  ${YELLOW}Skipped: $skipped$RESET")
        println("  Total: $total")
        println("  Duration: ${formatDuration(totalTime)}\n")

        if (failed > 0) {
            println("${RED}$BOLD❌ $failed test(s) failed$RESET")
        } else {
            println("${GREEN}$BOLD✅ All tests passed$RESET")
        }
    }

    fun allTestsPassed(): Boolean = results.none { it.status == Status.FAILED }

    fun getResults(): List<TestResult> = results.toList()

    private fun formatDuration(ms: Long): String =
        when {
            ms < 1000 -> "${ms}ms"
            ms < 60_000 -> "%.2fs".format(ms / 1000.0)
            else -> "%.2fm".format(ms / 60_000.0)
        }

    companion object {
        private val RESET = "\u001B[0m"
        private val RED = "\u001B[31m"
        private val GREEN = "\u001B[32m"
        private val YELLOW = "\u001B[33m"
        private val BLUE = "\u001B[34m"
        private val BOLD = "\u001B[1m"
    }
}
