# AI Usage Guide for `io.github.bhargavms.ksp-test-runner`

Purpose: teach an AI how to wire, configure, and run the KSP test runner plugin in Gradle builds.

## What the plugin provides
- Gradle plugin id: `io.github.bhargavms.ksp-test-runner`
- DSL extension: `kspTests { ... }`
- Task: `runKspTests`
- Optional property: `-PkspTestFilter=<regex>` to filter suite/test names.
- You must add `dependsOn` yourself if your build needs `kspKotlin`/`compileKotlin` before tests.

## How to apply
```kotlin
plugins {
    id("io.github.bhargavms.ksp-test-runner")
}
```

## How to define tests
```kotlin
kspTests {
    suite("My Suite") {
        // Mandataory: per-suite source set (maps to build/generated/ksp/<name>/kotlin)
        sourceSet("integration")
        // Or set explicit dir:
        // generatedDir("/abs/path/to/generated")

        beforeAll { /* setup */ }
        beforeEach { /* per-test setup */ }
        afterEach { /* per-test cleanup */ }
        afterAll { /* teardown */ }

        test("happy path") {
            val generated = generatedDir() // defaults to build/generated/ksp/main/kotlin unless overridden
            Assertions.assertTrue(generated.exists())
        }

        ftest("focused only") { /* only runs when any focused tests exist */ }
        xtest("skipped for now", reason = "not ready") { /* won’t run */ }
    }
}
```

## Assertions available (package `io.github.bhargavms.gradle.testing.Assertions`)
- Equality: `assertEquals`, `assertNotEquals`
- Booleans: `assertTrue`, `assertFalse`
- Nullability: `assertNull`, `assertNotNull`
- Files: `assertFileExists`, `assertFileNotExists`, `assertFileContentEquals`
- Text: `assertContentEquals`, `assertContains`, `assertNotContains`, `assertMatches`, `assertNotMatches`
- Exceptions: `assertThrows<T> {}`, `assertDoesNotThrow {}``

## How to run
```bash
./gradlew runKspTests
```

# Optional wiring for KSP/compile tasks
```kotlin
tasks.named("runKspTests") {
    dependsOn("kspKotlin") // or compileKotlin if required
}
```

Filter by name (regex):
```bash
./gradlew runKspTests -PkspTestFilter="My Suite|happy"
```

## Output behavior
- Prints dot/emoji progress with colors (green pass, red fail, yellow skip, blue start).
- Shows failures with message and trimmed stack trace.
- Summarizes passed/failed/skipped counts and total duration.

