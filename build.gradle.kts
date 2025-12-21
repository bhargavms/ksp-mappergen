plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ci.pipelines)
}

tasks.register<Exec>("ktlintCheck") {
    group = "verification"
    description = "Check Kotlin code style with ktlint"
    executable = "./scripts/ktlintw"
    args("**/*.kt", "**/*.kts")
}

tasks.register<Exec>("ktlintFormat") {
    group = "formatting"
    description = "Format Kotlin code with ktlint"
    executable = "./scripts/ktlintw"
    args("-F", "**/*.kt", "**/*.kts")
}
