plugins {
    id("org.cqfn.save.buildutils.kotlin-library")  // todo: disable publishing
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":save-common"))
                api("com.squareup.okio:okio-multiplatform:${Versions.okio}")
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
        val jvmTest by getting {
            dependsOn(jvmMain)
        }
        val nativeMain by getting
        val nativeTest by getting {
            dependsOn(nativeMain)
        }
    }
}
