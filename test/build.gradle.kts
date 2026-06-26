plugins {
    id("mappergen.kotlin-library")
    alias(libs.plugins.ksp)
}

// Sample uses JVM 21 - consumers choose their own version
kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(project(":annotations"))
    ksp(project(":mappergen"))

    // Test dependencies
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("known-bug")
    }
}

tasks.register<Test>("knownBugTest") {
    group = "red-tests"
    description = "Run RED-by-design tests tagged known-bug (allowed to fail)"
    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath
    useJUnitPlatform {
        includeTags("known-bug")
    }
}
