/**
 * Configuration for diktat static analysis
 */

package org.cqfn.save.buildutils

import org.cqfn.diktat.plugin.gradle.DiktatExtension
import org.cqfn.diktat.plugin.gradle.DiktatGradlePlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * Applies diktat gradle plugin and configures diktat for [this] project
 */
fun Project.configureDiktat() {
    apply<DiktatGradlePlugin>()
    configure<DiktatExtension> {
        diktatConfigFile = rootProject.file("diktat-analysis.yml")
        inputs {
            include("src/**/*.kt", "*.kts", "src/**/*.kts")
            exclude("$projectDir/build/**")
        }
    }
}

/**
 * Creates unified tasks to run diktat on all projects
 */
fun Project.createDiktatTask() {
    if (this == rootProject) {
        // apply diktat to buildSrc
        apply<DiktatGradlePlugin>()
        configure<DiktatExtension> {
            diktatConfigFile = rootProject.file("diktat-analysis.yml")
            // FixMe: temporary before the release 1.0.3 of diktat
            // reporterType = "sarif"
            inputs {
                include(
                    "$rootDir/buildSrc/src/**/*.kt",
                    "$rootDir/buildSrc/src/**/*.kts",
                    "$rootDir/*.kts",
                    "$rootDir/buildSrc/*.kts"
                )
                exclude("$rootDir/build", "$rootDir/buildSrc/build")
            }
        }
    }
    tasks.register("diktatCheckAll") {
        allprojects {
            tasks.findByName("diktatCheck")?.let { this@register.dependsOn(it) }
        }
    }
    tasks.register("diktatFixAll") {
        allprojects {
            tasks.findByName("diktatFix")?.let { this@register.dependsOn(it) }
        }
    }

    // FixMe: temporary before the release 1.0.3 of diktat
    /* this.configurations.getByName("diktat").dependencies.add(
        this.dependencies.create("com.pinterest.ktlint:ktlint-reporter-sarif:0.43.2")
    ) */
}
