plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        create("ciPipelines") {
            id = "mappergen.ci-pipelines"
            implementationClass = "com.bhargavms.mappergen.ci.CiPipelinePlugin"
        }
    }
}
