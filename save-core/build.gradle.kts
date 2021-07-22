
import org.cqfn.save.buildutils.configurePublishing
import org.cqfn.save.generation.configFilePath
import org.cqfn.save.generation.generateConfigOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
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
            }
        }
    }
    val hostTarget = listOf(linuxX64(), mingwX64(), macosX64())

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
        }
        val commonNonJsMain by creating {
            dependencies {
                implementation(project(":save-common"))
                implementation(project(":save-reporters"))
                api("com.squareup.okio:okio-multiplatform:${Versions.okio}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.Kotlinx.serialization}")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:${Versions.Kotlinx.cli}")
                implementation("com.akuleshov7:ktoml-core:${Versions.ktoml}")
                implementation(project(":save-plugins:fix-plugin"))
                implementation(project(":save-plugins:fix-and-warn-plugin"))
                implementation(project(":save-plugins:warn-plugin"))
            }
        }
        val commonNonJsTest by creating {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val nativeMain by creating {
            dependsOn(commonNonJsMain)
        }
        hostTarget.forEach {
            getByName("${it.name}Main").dependsOn(nativeMain)
        }

        val nativeTest by creating {
            dependsOn(commonNonJsTest)
        }
        hostTarget.forEach {
            getByName("${it.name}Test").dependsOn(nativeTest)
        }

        val jvmMain by getting {
            dependsOn(commonNonJsMain)
        }
        val jvmTest by getting {
            dependsOn(commonNonJsTest)
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

val generateConfigOptionsTaskProvider = tasks.register("generateConfigOptions") {
    inputs.file(configFilePath())
    val generatedFile = File("$buildDir/generated/src/org/cqfn/save/core/config/SaveProperties.kt")
    outputs.file(generatedFile)

    doFirst {
        generateConfigOptions(generatedFile)
    }
}
val generateVersionFileTaskProvider = tasks.register("generateVersionsFile") {
    inputs.property("project version", version.toString())
    val versionsFile = File("$buildDir/generated/src/org/cqfn/save/core/config/Versions.kt")
    outputs.file(versionsFile)

    doFirst {
        versionsFile.parentFile.mkdirs()
        versionsFile.writeText(
            """
            package org.cqfn.save.core.config

            internal const val SAVE_VERSION = "$version"

            """.trimIndent()
        )
    }
}
val generatedKotlinSrc = kotlin.sourceSets.create("generated") {
    kotlin.srcDir("$buildDir/generated/src")
    dependencies {
        implementation(project(":save-common"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.Kotlinx.serialization}")
        implementation("org.jetbrains.kotlinx:kotlinx-cli:${Versions.Kotlinx.cli}")
    }
}
kotlin.sourceSets.getByName("commonMain").dependsOn(generatedKotlinSrc)
tasks.withType<KotlinCompile<*>>().forEach {
    it.dependsOn(generateConfigOptionsTaskProvider)
    it.dependsOn(generateVersionFileTaskProvider)
}
