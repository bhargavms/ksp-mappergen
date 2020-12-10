plugins {
    kotlin("jvm")
}

dependencies {
    implementation(Libs.kotlinStdLib)
    implementation(Libs.googleKSP)
    implementation(project(":mapperGenAnnotations"))
    implementation(project(":codegen"))

    testApi(project(":testCore"))
}
