import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"

}

group = "org.cqfn"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx/")
}



kotlin {
    jvm()
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val saveTarget = when {
        hostOs == "Mac OS X" -> macosX64("save")
        hostOs == "Linux" -> linuxX64("save")
        isMingwX64 -> mingwX64("save")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    saveTarget.apply {
        binaries {
            executable {
                entryPoint = "org.cqfn"
            }
        }
    }

    sourceSets {

        val saveMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio-multiplatform:3.0.0-alpha.1")
                implementation( "org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
            }
        }
        val saveTest by getting

        val jvmMain by getting {
            dependsOn(saveMain)
        }

        val jvmTest by getting {
            dependsOn(saveTest)
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:5.0.0")
            }
        }
    }
}


tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}

