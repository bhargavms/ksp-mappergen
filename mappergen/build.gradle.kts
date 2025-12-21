plugins {
    id("mappergen.kotlin-library")
}

// Publishing coordinates for the main library
group = "com.bhargavms.mappergen"
version = "0.1.0-SNAPSHOT"

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":annotations"))
    implementation(project(":codegen"))
}
