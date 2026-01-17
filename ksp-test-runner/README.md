# KSP Test Runner Plugin

JUnit-like DSL for KSP integration testing with suites, lifecycle hooks, assertions, and colorized reporting. Published under plugin id `io.github.bhargavms.ksp-test-runner`.

## Apply the plugin
```kotlin
plugins {
    id("io.github.bhargavms.ksp-test-runner")
}
```

## Define tests (DSL)
```kotlin
kspTests {
    suite("Basic Mappers") {
        beforeAll { /* setup */ }
        afterAll { /* teardown */ }

        test("generates expected file") {
            val generated = generatedDir().resolve("UserMapper.kt")
            Assertions.assertFileExists(generated)
        }

        test("content matches fixture") {
            val generated = generatedDir().resolve("UserMapper.kt")
            val expected = file("src/testFixtures/kotlin/UserMapper.kt")
            Assertions.assertFileContentEquals(expected, generated)
        }
    }
}
```

## Run tests
```bash
./gradlew runKspTests
```

If your build requires KSP/compile before running the DSL, wire it explicitly:
```kotlin
tasks.named("runKspTests") {
    dependsOn("kspKotlin") // or compileKotlin if needed
}
```

## TODO
- Make the runner configuration-cache compatible (record specs instead of capturing build-script lambdas; avoid Task.project at execution).

### Filter tests (regex)
```bash
./gradlew runKspTests -PkspTestFilter="Basic Mappers"
```

### Override generated directory (custom source sets)
```kotlin
kspTests {
    suite("Unit") {
        sourceSet("main") // build/generated/ksp/main/kotlin
        // or explicit:
        // generatedDir("/absolute/path/to/generated")
    }

    suite("Integration") {
        sourceSet("integration") // build/generated/ksp/integration/kotlin
    }
}
```

### Assertions available
- `assertEquals`, `assertNotEquals`, `assertTrue`, `assertFalse`
- `assertNull`, `assertNotNull`
- `assertFileExists`, `assertFileNotExists`, `assertFileContentEquals`
- `assertContentEquals`, `assertContains`, `assertNotContains`, `assertMatches`, `assertNotMatches`
- `assertThrows<T> { }`, `assertDoesNotThrow { }`
