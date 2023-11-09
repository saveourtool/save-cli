plugins {
    id("com.saveourtool.save.buildutils.kotlin-library")
}

kotlin {
    js(IR) {
        browser()
        testRuns.configureEach {
            filter {
                setExcludePatterns(
                    "*ExtraFlagsExtractorTest*",
                    "*ProcessBuilderTest",
                    "*FileUtilsTest*",
                )
            }
        }
    }

    // store yarn.lock in the root directory
    rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension> {
        lockFileDirectory = rootProject.projectDir
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.okio)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.okio.fakefilesystem)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
                implementation(kotlin("test-js-runner"))
            }
        }
    }
}
