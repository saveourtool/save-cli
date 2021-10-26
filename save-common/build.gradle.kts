plugins {
    id("org.cqfn.save.buildutils.kotlin-library")
}

kotlin {
    // additionally, save-common should be available for JS too
    js(BOTH).browser()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.squareup.okio:okio:${Versions.okio}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.Kotlinx.serialization}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.Kotlinx.datetime}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("com.squareup.okio:okio-fakefilesystem:${Versions.okio}")
            }
        }
    }
}
