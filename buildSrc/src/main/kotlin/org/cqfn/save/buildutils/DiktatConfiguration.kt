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
        inputs = files("src/**/*.kt", "*.kts", "src/**/*.kts")
        excludes = files("$projectDir/build")
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
            inputs = files(
                "$rootDir/buildSrc/src/**/*.kt",
                "$rootDir/buildSrc/src/**/*.kts",
                "$rootDir/*.kts",
                "$rootDir/buildSrc/*.kts"
            )
            excludes = files("$rootDir/build", "$rootDir/buildSrc/build")
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
}
