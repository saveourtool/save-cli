import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    kotlin("multiplatform")
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

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":save-core"))
            }
        }
    }
}
