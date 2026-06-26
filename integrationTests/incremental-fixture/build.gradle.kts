plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    ksp("com.bhargavms.mappergen:mappergen")
    compileOnly("com.bhargavms.mappergen:annotations")
}
