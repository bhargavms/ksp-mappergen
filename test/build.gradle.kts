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
