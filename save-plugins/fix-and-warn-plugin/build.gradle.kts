import org.cqfn.save.buildutils.configurePublishing

plugins {
    `kotlin-library`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":save-common"))
                implementation(project(":save-plugins:fix-plugin"))
                implementation(project(":save-plugins:warn-plugin"))
                implementation("io.github.petertrr:kotlin-multiplatform-diff:${Versions.multiplatformDiff}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.Kotlinx.serialization}")
            }
        }
    }
}
