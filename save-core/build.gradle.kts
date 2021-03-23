import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx/")
}

kotlin {
    jvm()
    val os = getCurrentOperatingSystem()
    val saveTarget = when {
        os.isMacOsX -> macosX64("save")
        os.isLinux -> linuxX64("save")
        os.isWindows -> mingwX64("save")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    configure(listOf(saveTarget)) {
    }

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
        }
        val commonMain by getting {
            dependencies {
                api("com.squareup.okio:okio-multiplatform:3.0.0-alpha.1")
            }
        }
        val commonTest by getting

        val jvmMain by getting

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
            }
        }
    }
}

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
