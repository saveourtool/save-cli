/**
 * Configuration for detekt static analysis
 */

package org.cqfn.save.buildutils

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Configure Detekt for a single project
 */
fun Project.configureDetekt() {
    apply<DetektPlugin>()
    configure<DetektExtension> {
        config = rootProject.files("detekt.yml")
        buildUponDefaultConfig = true
    }
    // extremely awful hack to get to the point, when `DetektMultiplatform` has registered all the tasks
    afterEvaluate {
        // detekt registers tasks after Kotlin plugin has set up all targets
        afterEvaluate {
            // but they also use a nested `afterEvaluate` for interop with Android Gradle Plugin
            afterEvaluate {
                // so we need a third `afterEvaluate`, so that all detekt tasks are already registered
                tasks.matching { it.name == "check" }.configureEach {
                    dependsOn.removeIf {
                        it is TaskProvider<*> && it.name.startsWith("detekt")
                    }
                }
            }
        }
    }
}

/**
 * Register a unified detekt task
 */
fun Project.createDetektTask() {
    tasks.register("detektAll") {
        allprojects {
            this@register.dependsOn(tasks.withType<Detekt>())
        }
    }
}
