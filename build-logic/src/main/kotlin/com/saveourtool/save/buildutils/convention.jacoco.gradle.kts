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

plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.7"
}

    val kotlin: KotlinMultiplatformExtension = extensions.getByType()
    val jvmTestTask = tasks.named<Test>("jvmTest") {
        configure<JacocoTaskExtension> {
            // this is needed to generate jacoco/jvmTest.exec
            isEnabled = true
        }
    }
    val jacocoTestReportTask = tasks.register<JacocoReport>("jacocoTestReport") {
//        executionData(jvmTestTask.extensions.getByType(JacocoTaskExtension::class.java).destinationFile)
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
    jvmTestTask.configure {
        finalizedBy(jacocoTestReportTask)
    }
    jacocoTestReportTask.configure {
        dependsOn(jvmTestTask)
    }
