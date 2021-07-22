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

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
        }
        val commonNonJsTest by creating {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmTest by getting {
            dependsOn(commonNonJsTest)
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
            }
        }
        nativeTargets.forEach {
            getByName("${it.name}Test").dependsOn(commonNonJsTest)
        }
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}
