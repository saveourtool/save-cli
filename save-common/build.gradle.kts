import org.cqfn.save.buildutils.configurePublishing
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
                freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
            }
        }
    }
    val hostTarget = listOf(linuxX64(), mingwX64(), macosX64())

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
        }
        val commonMain by getting {
            dependencies {
                api("com.squareup.okio:okio-multiplatform:${Versions.okio}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.Kotlinx.serialization}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.Kotlinx.datetime}")
            }
        }
        val commonNonJsTest by creating {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        hostTarget.forEach {
            getByName("${it.name}Main").dependsOn(nativeMain)
        }

        val nativeTest by creating {
            dependsOn(commonNonJsTest)
        }
        hostTarget.forEach {
            getByName("${it.name}Test").dependsOn(nativeTest)
        }

        val jvmTest by getting {
            dependsOn(commonNonJsTest)
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
            }
        }
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
