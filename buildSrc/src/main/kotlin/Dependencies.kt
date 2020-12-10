const val kotlinVersion = "1.4.10"

object BuildPlugins {

    object Versions {
        const val buildToolsVersion = "4.1.0"
    }

    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.buildToolsVersion}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"

    const val androidApplication = "com.android.application"
    const val kotlinAndroid = "kotlin-android"
}

object Libs {
    const val jUnit = "junit:junit:${Versions.jUnit}"
    const val mockitoCore = "org.mockito:mockito-core:${Versions.mockito}"
    const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    const val googleKSP = "com.google.devtools.ksp:symbol-processing-api:${Versions.googleKSP}"
    const val hamcrest = "org.hamcrest:hamcrest:${Versions.hamcrest}"
    const val kotlinPoet = "com.squareup:kotlinpoet:${Versions.kotlinPoet}"

    private object Versions {
        const val jUnit = "4.12"
        const val mockito = "2.23.0"
        const val mockitoKotlin = "2.2.0"
        const val googleKSP = "1.4.10-dev-experimental-20201106"
        const val hamcrest = "2.2"
        const val kotlinPoet = "1.7.2"
    }
}

object AndroidSdk {
    const val min = 16
    const val compile = 29
    const val target = compile
    const val buildToolsVersion = "29.0.3"
}
