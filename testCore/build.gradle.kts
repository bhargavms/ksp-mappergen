plugins {
    kotlin("jvm")
}

dependencies {
    implementation(Libs.kotlinStdLib)
    api(Libs.hamcrest)
    api(Libs.jUnit)
    api(Libs.mockitoCore)
    api(Libs.mockitoKotlin)
}
