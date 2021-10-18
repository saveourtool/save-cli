plugins {
    id("org.cqfn.save.buildutils.kotlin-library")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.saveCommon)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.Kotlinx.serialization}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.Kotlinx.serialization}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.saveCommonTest)
            }
        }
    }
}
