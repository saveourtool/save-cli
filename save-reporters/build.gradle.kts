import org.cqfn.save.buildutils.configurePublishing

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
                implementation(project(":save-core"))
            }
        }
    }
}

configurePublishing()

tasks.withType<org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest> {
    useJUnitPlatform()
}
