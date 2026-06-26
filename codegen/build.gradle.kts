import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    id("mappergen.kotlin-library")
    alias(libs.plugins.kover)
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(kotlin("reflect"))

    testImplementation(project(":mappergen"))
    testImplementation(project(":annotations"))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.api)
    testImplementation(libs.kctfork.core)
    testImplementation(libs.kctfork.ksp)
    testRuntimeOnly(libs.junit.engine)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("known-bug")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (name == "compileTestKotlin") {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        }
    }
}

tasks.register<Test>("knownBugTest") {
    group = "red-tests"
    description = "Run RED-by-design tests tagged known-bug (allowed to fail)"
    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath
    useJUnitPlatform {
        includeTags("known-bug")
    }
}

kover {
    currentProject {
        instrumentation {
            disabledForTestTasks.add("knownBugTest")
        }
    }
    reports {
        filters {
            excludes {
                classes(
                    "com.bhargavms.mappergen.code.generator.utils.KSTypeExtKt",
                    "com.bhargavms.mappergen.code.generator.error.UndefinedClass",
                    "com.bhargavms.mappergen.code.generator.ContractKt",
                    "com.bhargavms.mappergen.code.generator.MapperFile",
                    "com.bhargavms.mappergen.code.generator.MapFunction",
                    "com.bhargavms.mappergen.code.generator.IterableObjectMapFunction",
                    "com.bhargavms.mappergen.code.generator.Assignment\$Companion",
                )
            }
        }
        total {
            log {
                onCheck = false
            }
            verify {
                rule {
                    bound {
                        minValue = 90
                        coverageUnits = CoverageUnit.LINE
                    }
                    bound {
                        minValue = 68
                        coverageUnits = CoverageUnit.BRANCH
                    }
                }
            }
        }
    }
}
