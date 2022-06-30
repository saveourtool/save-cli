/**
 * Configure JaCoCo for code coverage calculation
 */

package com.saveourtool.save.buildutils

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Calculate code coverage from JVM test executions.
 */
fun Project.configureJacoco() {
    apply<JacocoPlugin>()

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.8"
    }

    val kotlin: KotlinMultiplatformExtension = extensions.getByType()
    val jvmTestTask by tasks.named<Test>("jvmTest") {
        configure<JacocoTaskExtension> {
            // this is needed to generate jacoco/jvmTest.exec
            isEnabled = true
        }
    }

    val configure: JacocoReport.() -> Unit = {
        executionData(jvmTestTask.extensions.getByType(JacocoTaskExtension::class.java).destinationFile)
        // todo: include platform-specific source sets
        additionalSourceDirs(kotlin.sourceSets["commonMain"].kotlin.sourceDirectories)
        classDirectories.setFrom(fileTree("$buildDir/classes/kotlin/jvm/main").apply {
            exclude("**/*\$\$serializer.class")
        })
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    // `application` plugin creates jacocoTestReport task in plugin section (this is definitely incorrect behavior)
    // AFTER that in "com.saveourtool.save.buildutils.kotlin-library" we try to register this task once again and fail
    // so the order of plugins in `apply` is critically important
    val jacocoTestReportTask = if (project.name == "save-cli") {
        val jacocoTestReportTask by tasks.named("jacocoTestReport", configure)
        jacocoTestReportTask
    } else {
        val jacocoTestReportTask by tasks.register("jacocoTestReport", configure)
        jacocoTestReportTask
    }

    jvmTestTask.finalizedBy(jacocoTestReportTask)
    jacocoTestReportTask.dependsOn(jvmTestTask)
}
