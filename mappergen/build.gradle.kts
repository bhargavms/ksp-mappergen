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

val codegenJar = project(":codegen").tasks.named<Jar>("jar")

tasks.named<Jar>("jar") {
    from(codegenJar.flatMap { it.archiveFile }.map { zipTree(it.asFile) }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}
