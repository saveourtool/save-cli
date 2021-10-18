

plugins {
    id("org.cqfn.save.buildutils.kotlin-library")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.saveCommon)
                implementation(projects.savePlugins.fixPlugin)
                implementation(projects.savePlugins.warnPlugin)
                implementation("io.github.petertrr:kotlin-multiplatform-diff:${Versions.multiplatformDiff}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.Kotlinx.serialization}")
            }
        }
    }
}
