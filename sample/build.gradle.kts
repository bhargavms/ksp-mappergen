plugins {
    id("mappergen.kotlin-library")
    alias(libs.plugins.ksp)
    application
}

// Sample uses JVM 21 - consumers choose their own version
kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.example.sample.MainKt")
}

dependencies {
    compileOnly(project(":annotations"))
    ksp(project(":mappergen"))
}
