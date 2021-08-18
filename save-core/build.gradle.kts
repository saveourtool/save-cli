
import org.cqfn.save.generation.configFilePath
import org.cqfn.save.generation.generateConfigOptions

import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    id("org.cqfn.save.buildutils.kotlin-library")
    id("de.undercouch.download")
}

kotlin {
    sourceSets {
        val commonNonJsMain by getting {
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
        val commonNonJsTest by getting {
            dependencies {
                implementation(project(":save-common-test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
                implementation("io.ktor:ktor-client-core:${Versions.ktorVersion}")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("io.ktor:ktor-client-apache:${Versions.ktorVersion}")
            }
        }

        val nativeTest by getting {
            dependencies {
                implementation("io.ktor:ktor-client-curl:${Versions.ktorVersion}")
            }
        }
    }
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
kotlin.sourceSets.getByName("commonNonJsMain").dependsOn(generatedKotlinSrc)
tasks.withType<KotlinCompile<*>>().forEach {
    it.dependsOn(generateConfigOptionsTaskProvider)
    it.dependsOn(generateVersionFileTaskProvider)
}

val ktlintVersion = "0.39.0"
val diktatVersion = "1.0.0-rc.2"
tasks.register<Download>("downloadTestResources") {
    src(listOf(
        "https://github.com/pinterest/ktlint/releases/download/$ktlintVersion/ktlint",
        "https://github.com/cqfn/diKTat/releases/download/v$diktatVersion/diktat-$diktatVersion.jar"
    ))
    dest("..examples/kotlin-diktat")
    doLast {
        copy {
            from("..examples/kotlin-diktat/diktat-$diktatVersion.jar")
            into("..examples/kotlin-diktat/diktat.jar")
        }
    }
}
val cleanupTask = tasks.register("cleanupTestResources") {
    mustRunAfter(tasks.withType<Test>())
    doFirst {
        file("..examples/kotlin-diktat/ktlint").delete()
        file("..examples/kotlin-diktat/diktat.jar").delete()
    }
}
tasks.withType<Test>().configureEach {
    dependsOn("downloadTestResources")
    finalizedBy("cleanupTestResources")
}
