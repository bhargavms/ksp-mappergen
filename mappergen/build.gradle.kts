import org.gradle.api.tasks.bundling.Jar

plugins {
    id("mappergen.kotlin-library")
    id("mappergen.publish")
}

dependencies {
    compileOnly(libs.ksp.api)
    implementation(project(":annotations"))
    compileOnly(project(":codegen"))
    implementation(libs.kotlinpoet)
    implementation(kotlin("reflect"))
}

// Embed codegen classes in this jar (no published codegen artifact).
// Use source-set output, not zipTree(project.file), so configuration cache can serialize it.
val codegenMain =
    project(":codegen")
        .extensions
        .getByType<SourceSetContainer>()
        .named("main")

tasks.named<Jar>("jar") {
    from(codegenMain.map { it.output })
}
