/**
 * Utility functions to configure Kotlin in Gradle
 */

package org.cqfn.save.buildutils

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

/**
 * Some Kotlin Multiplatform targets are available on multiple OS.
 *
 * @throws GradleException if executed on some exotic OS
 */
fun Project.disableRedundantKmpTargets() {
    if (hasProperty("disableRedundantTargets") && (property("disableRedundantTargets") as String).toBoolean()) {
        afterEvaluate {
            val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
            val redundantTarget: String? = when {
                currentOs.isWindows -> "linuxX64"
                currentOs.isMacOsX -> "linuxX64"
                currentOs.isLinux -> null
                else -> throw GradleException("Unknown operating system ${currentOs.name}")
            }
            tasks.matching { redundantTarget != null && it.name.contains(redundantTarget, ignoreCase = true) }
                .configureEach {
                    enabled = false
                }
        }
    }
}
