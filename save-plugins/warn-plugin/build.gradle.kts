plugins {
    id("com.saveourtool.save.buildutils.kotlin-library")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.saveCommon)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.sarif4k)
            }
        }
    }
}
