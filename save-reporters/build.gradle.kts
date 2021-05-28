import org.cqfn.save.buildutils.configurePublishing
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()
    mingwX64()
    macosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":save-common"))
            }
        }
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
