

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
                implementation(libs.sarif.utils)
                implementation(libs.log4j.core)
                implementation(libs.log4j.slf4j2.impl)
            }
        }
    }
}
