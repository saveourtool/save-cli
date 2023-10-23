/**
 * Precompiled script plugin, that applies common configuration for a KMP project.
 * It specifies common targets and sets some common compiler flags.
 * It creates a number of useful source sets and adds common dependencies.
 * These source sets can be retrieved in a particular build script and configured further as needed.
 */

package com.saveourtool.save.buildutils

import org.gradle.kotlin.dsl.kotlin
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
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
    val nativeTargets = listOf(
        linuxX64(),
        mingwX64(),
        macosX64(),
        /* macosArm64() */
    )
    if (project.name == "save-common") {
        // additionally, save-common should be available for JS too
        // fixme: shouldn't rely on hardcoded project name here
        js(IR).browser()

        // store yarn.lock in the root directory
        rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension> {
            lockFileDirectory = rootProject.projectDir
        }
    }

    if (hasProperty("disableRedundantTargets") && (property("disableRedundantTargets") as String?) != "false") {
        // with this flag we exclude targets that are present on multiple OS to speed up build
        val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
        val redundantTarget: String? = when {
            currentOs.isWindows -> "linuxX64"
            currentOs.isMacOsX -> "linuxX64"
            currentOs.isLinux -> null
            else -> throw GradleException("Unknown operating system ${currentOs.name}")
        }
        tasks.matching { redundantTarget != null && it.name.contains(redundantTarget, ignoreCase = true) }
            .configureEach {
                logger.lifecycle("Disabling task :${project.name}:$name on host $currentOs")
                enabled = false
            }
    }

    /*
     * Common structure for MPP libraries:
     *            common
     *              |
     *            nonJs
     *          /       \
     *       native      JVM
     *     /   |    \
     * linux  mingw macos
     */
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation("io.kotest:kotest-assertions-core:5.6.2")
            }
        }
        val commonNonJsMain by creating {
            dependsOn(commonMain)
        }
        val commonNonJsTest by creating {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependsOn(commonNonJsMain)
        }
        val jvmTest by getting {
            dependsOn(commonNonJsTest)
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
            }
        }
        val nativeMain by creating {
            dependsOn(commonNonJsMain)
        }
        val nativeTest by creating {
            dependsOn(commonNonJsTest)
        }
        nativeTargets.forEach {
            getByName("${it.name}Main").dependsOn(nativeMain)
        }
        nativeTargets.forEach {
            getByName("${it.name}Test").dependsOn(nativeTest)
        }
    }
}

configureJacoco()
configurePublishing()
configureDiktat()
configureDetekt()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
