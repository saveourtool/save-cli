

plugins {
    id("com.saveourtool.save.buildutils.kotlin-library")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.saveCommon)
                implementation(libs.multiplatform.diff)
                implementation(libs.kotlinx.serialization.core)
            }
        }
    }
}
