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
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    val os = getCurrentOperatingSystem()
    val saveTarget = when {
        os.isMacOsX -> macosX64()
        os.isLinux -> linuxX64()
        os.isWindows -> mingwX64()
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
                api("com.squareup.okio:okio-multiplatform:${Versions.okio}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        getByName("${saveTarget.name}Main").dependsOn(nativeMain)

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
