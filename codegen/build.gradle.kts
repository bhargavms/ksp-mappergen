plugins {
    id("mappergen.kotlin-library")
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(kotlin("reflect"))
}
