/**
 * Version configuration file.
 */

@file:Suppress("UNUSED_IMPORT")

package org.cqfn.save.buildutils

import com.palantir.gradle.gitversion.GitVersionPlugin
import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate

internal val tagPattern = Regex("""^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*).*$""")

/**
 * Configures how project version is determined. We are using `git-version` plugin to get a version from git repo.
 * If working tree is dirty (i.e. there are uncommitted or untracked changes) and release build is attempted, build will fail.
 *
 * @throws GradleException if there was an attempt to run release build with dirty working tree
 */
fun Project.configureVersioning() {
    require(this == rootProject) { "Versioning should be configured for the root project" }

    apply<GitVersionPlugin>()

    val versionDetails: Closure<VersionDetails> by extra
    val details = versionDetails.invoke()

    require(tagPattern.matches(details.lastTag)) {
        "Git tag ${details.lastTag} doesn't match the required pattern ${tagPattern.pattern}"
    }

    allprojects {
        version = details.version.trim('v')
    }
    logger.lifecycle("Discovered version $version, working tree is ${if (details.isCleanTag) "clean" else "dirty"}")

    // to activate release, provide `-Prelease` or `-Prelease=true`. To deactivate, either omit the property, or set `-Prelease=false`.
    val isRelease = hasProperty("release") && (property("release") as String != "false")
    if (isRelease && !details.isCleanTag) {
        throw GradleException("Release build will be performed with not clean git tree; aborting.")
    }
}
