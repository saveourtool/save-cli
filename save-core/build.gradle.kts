
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
                implementation(projects.saveCommon)
                implementation(projects.saveReporters)
                api(libs.okio)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.cli)
                implementation(libs.ktoml.core)
                implementation(libs.ktoml.file)
                implementation(projects.savePlugins.fixPlugin)
                implementation(projects.savePlugins.fixAndWarnPlugin)
                implementation(projects.savePlugins.warnPlugin)
            }
        }
        val commonNonJsTest by getting {
            dependencies {
                implementation(projects.saveCommonTest)
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlinx.coroutines.core)
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
kotlin.sourceSets.getByName("commonNonJsMain") {
    kotlin.srcDir("$buildDir/generated/src")
}
tasks.withType<KotlinCompile<*>>().forEach {
    it.dependsOn(generateConfigOptionsTaskProvider)
    it.dependsOn(generateVersionFileTaskProvider)
}

tasks.register<Download>("downloadOldTestResources") {
    src(listOf(
        Versions.IntegrationTest.oldKtlintLink,
        Versions.IntegrationTest.oldDiktatLink,
    ))
    dest("../examples/kotlin-diktat")
    doLast {
        file("../examples/kotlin-diktat/diktat-${Versions.IntegrationTest.oldDiktat}.jar").renameTo(
            file("../examples/kotlin-diktat/diktat-old.jar")
        )

        file("../examples/kotlin-diktat/ktlint").renameTo(
            file("../examples/kotlin-diktat/ktlint-old")
        )
    }
}

tasks.register<Download>("downloadTestResources") {
    src(listOf(
        Versions.IntegrationTest.ktlintLink,
        Versions.IntegrationTest.diktatLink,
    ))
    dest("../examples/kotlin-diktat")
    doLast {
        file("../examples/kotlin-diktat/diktat-${Versions.IntegrationTest.diktat}.jar").renameTo(
            file("../examples/kotlin-diktat/diktat.jar")
        )
    }
}

val cleanupTask = tasks.register("cleanupTestResources") {
    this.dependsOn(":save-cli:jvmTest")
    mustRunAfter(tasks.withType<Test>())
    doFirst {
        file("../examples/kotlin-diktat/ktlint-old").delete()
        file("../examples/kotlin-diktat/ktlint").delete()
        file("../examples/kotlin-diktat/diktat-old.jar").delete()
        file("../examples/kotlin-diktat/diktat.jar").delete()
    }
}
tasks.withType<Test>().configureEach {
    dependsOn("downloadTestResources")
    dependsOn("downloadOldTestResources")
    finalizedBy("cleanupTestResources")
}
