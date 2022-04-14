package org.cqfn.save.buildutils

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
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
        toolVersion = "0.8.7"
    }

    val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
    val jvmTestTask by tasks.named<Test>("jvmTest") {
        configure<JacocoTaskExtension> {
            // this is needed to generate jacoco/jvmTest.exec
            isEnabled = true
        }
    }
    val jacocoTestReportTask by tasks.register<JacocoReport>("jacocoTestReport") {
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
    jvmTestTask.finalizedBy(jacocoTestReportTask)
    jacocoTestReportTask.dependsOn(jvmTestTask)
}
