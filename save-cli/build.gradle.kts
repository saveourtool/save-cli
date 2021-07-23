import org.cqfn.save.buildutils.configurePublishing

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    val os = getCurrentOperatingSystem()
    val saveTarget = listOf(when {
        os.isWindows -> mingwX64()
        os.isLinux -> linuxX64()
        os.isMacOsX -> macosX64()
        else -> throw GradleException("Unknown operating system $os")
    })

    configure(saveTarget) {
        binaries {
            val name = "save-${project.version}-${this@configure.name}"
            executable {
                this.baseName = name
                entryPoint = "org.cqfn.save.cli.main"
            }
        }
    }

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
        }
        val jvmMain by getting

        val commonMain by getting
        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(project(":save-core"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:${Versions.Kotlinx.serialization}")
            }
        }
        saveTarget.forEach {
            getByName("${it.name}Main").dependsOn(nativeMain)
        }
        val commonTest by getting

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
            }
        }
    }

    val linkReleaseExecutableTaskProvider = when {
        os.isLinux -> tasks.getByName("linkReleaseExecutableLinuxX64")
        os.isWindows -> tasks.getByName("linkReleaseExecutableMingwX64")
        os.isMacOsX -> tasks.getByName("linkReleaseExecutableMacosX64")
        else -> throw GradleException("Unknown operating system $os")
    }
    project.tasks.register("linkReleaseExecutableMultiplatform") {
        dependsOn(linkReleaseExecutableTaskProvider)
    }

    // disable building of some binaries to speed up build
    // possible values: `all` - build all binaries, `debug` - build only debug binaries
    val enabledExecutables = if (hasProperty("enabledExecutables")) property("enabledExecutables") as String else null
    if (enabledExecutables != null && enabledExecutables != "all" || enabledExecutables == "debug") {
        linkReleaseExecutableTaskProvider.enabled = false
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
