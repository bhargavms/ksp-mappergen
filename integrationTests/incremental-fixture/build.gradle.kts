plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    ksp("io.github.bhargavms:mappergen")
    compileOnly("io.github.bhargavms:mappergen-annotations")
}
