
import org.cqfn.save.generation.configFilePath
import org.cqfn.save.generation.generateConfigOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    `kotlin-library`
}

kotlin {
    sourceSets {
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
        val commonNonJsTest by getting {
            dependsOn(commonNonJsMain)
        }
        val nativeMain by getting {
            dependsOn(commonNonJsMain)
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
kotlin.sourceSets.getByName("commonMain").dependsOn(generatedKotlinSrc)
tasks.withType<KotlinCompile<*>>().forEach {
    it.dependsOn(generateConfigOptionsTaskProvider)
    it.dependsOn(generateVersionFileTaskProvider)
}
