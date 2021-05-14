import org.cqfn.save.buildutils.configurePublishing
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    val os = getCurrentOperatingSystem()
    val saveTarget = listOf(macosX64(), linuxX64(), mingwX64())

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
                implementation( "org.jetbrains.kotlinx:kotlinx-serialization-properties:${Versions.Kotlinx.serialization}")
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

    project.tasks.register("linkReleaseExecutableMultiplatform") {
        when {
            os.isLinux -> dependsOn(tasks.getByName("linkReleaseExecutableLinuxX64"))
            os.isWindows -> dependsOn(tasks.getByName("linkReleaseExecutableMingwX64"))
            //FixMe https://github.com/cqfn/save/issues/53
            //os.isMacOsX -> dependsOn(tasks.getByName("linkReleaseExecutableMacosX64"))
        }
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
