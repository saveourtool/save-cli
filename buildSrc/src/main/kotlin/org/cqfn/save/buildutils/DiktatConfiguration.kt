/**
 * Configuration for diktat static analysis
 */

package org.cqfn.save.buildutils

import org.cqfn.diktat.plugin.gradle.DiktatExtension
import org.cqfn.diktat.plugin.gradle.DiktatGradlePlugin
import org.cqfn.diktat.plugin.gradle.DiktatJavaExecTaskBase
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Applies diktat gradle plugin and configures diktat for [this] project
 */
fun Project.configureDiktat() {
    apply<DiktatGradlePlugin>()
    configure<DiktatExtension> {
        diktatConfigFile = rootProject.file("diktat-analysis.yml")
        githubActions = findProperty("diktat.githubActions")?.toString()?.toBoolean() ?: false
        inputs {
            if (path == rootProject.path) {
                include(
                    "$rootDir/buildSrc/src/**/*.kt",
                    "$rootDir/buildSrc/**/*.kts",
                    "$rootDir/*.kts"
                )
                exclude("$rootDir/build", "$rootDir/buildSrc/build")
            } else {
                include("src/**/*.kt", "*.kts", "src/**/*.kts")
                exclude("$projectDir/build/**")
            }
        }
    }
}

private fun Project.fixDiktatTask() {
    tasks.withType<DiktatJavaExecTaskBase>().configureEach {
        // https://github.com/analysis-dev/diktat/issues/1269
        systemProperty("user.home", rootDir.toString())
    }
}
