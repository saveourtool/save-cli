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
    val saveTarget = when {
        os.isMacOsX -> macosX64()
        os.isLinux -> linuxX64()
        os.isWindows -> mingwX64()
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    configure(listOf(saveTarget)) {
        binaries {
            executable {
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
        getByName("${saveTarget.name}Main").dependsOn(nativeMain)
        val commonTest by getting


        val jvmTest by getting {
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
