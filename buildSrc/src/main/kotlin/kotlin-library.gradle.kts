import org.cqfn.save.buildutils.configurePublishing
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
//import org.jetbrains.kotlin.gradle.dsl.jvm

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
    val nativeTargets = listOf(linuxX64(), mingwX64(), macosX64())

    /**
     * Common structure for MPP libraries:
     * common
     * |
     * nonJs
     * / \
     * native JVM
     * / | \
     * linux mingw macos
     */
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
        }
        val commonMain by getting
        val commonTest by getting
        val commonNonJsTest by creating {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
//                implementation("com.squareup.okio:okio-fakefilesystem-multiplatform:${Versions.okio}")
            }
        }
        val jvmTest by getting {
            dependsOn(commonNonJsTest)
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val nativeTest by creating {
            dependsOn(commonTest)
        }
        nativeTargets.forEach {
            getByName("${it.name}Main").dependsOn(nativeMain)
        }
        nativeTargets.forEach {
            getByName("${it.name}Test").dependsOn(nativeTest)
            getByName("${it.name}Test").dependsOn(commonNonJsTest)
        }
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
