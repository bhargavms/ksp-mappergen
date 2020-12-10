plugins {
    kotlin("jvm")
}

dependencies {
    implementation(Libs.kotlinStdLib)
    implementation(Libs.googleKSP)
    implementation(Libs.kotlinPoet)

    testApi(project(":testCore"))
    implementation(kotlin("reflect"))
}