

plugins {
    `kotlin-library`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":save-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.Kotlinx.serialization}")
            }
        }
    }
}
