plugins {
    id("com.saveourtool.save.buildutils.kotlin-library")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.saveCommon)
                implementation(libs.kotlinx.serialization.core)
                implementation("io.github.detekt.sarif4k:sarif4k:0.1.0-SNAPSHOT")
            }
        }
    }
}
