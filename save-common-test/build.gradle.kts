plugins {
    id("com.saveourtool.save.buildutils.kotlin-library")  // todo: disable publishing
}

kotlin {
    sourceSets {
        val commonNonJsMain by getting {
            dependencies {
                implementation(projects.saveCommon)
                api(libs.okio)
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
    }
}
