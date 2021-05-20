/**
 * Version configuration file.
 */

@file:Suppress("UNUSED_IMPORT")

package org.cqfn.save.buildutils

import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.ajoberstar.grgit.Grgit

internal val tagPattern = Regex("""^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*).*$""")

/**
 * Configures how project version is determined. We are using `git-version` plugin to get a version from git repo.
 * If working tree is dirty (i.e. there are uncommitted or untracked changes) and release build is attempted, build will fail.
 *
 * @throws GradleException if there was an attempt to run release build with dirty working tree
 */
fun Project.configureVersioning() {
    apply<ReckonPlugin>()

    configure<ReckonExtension> {
        scopeFromProp()
        stageFromProp("alpha", "rc", "final")  // version string will be based on last commit; when checking out a tag, that tag will be used
    }

    // to activate release, provide `-Prelease` or `-Prelease=true`. To deactivate, either omit the property, or set `-Prelease=false`.
    val isRelease = hasProperty("release") && (property("release") as String != "false")
    val grgit: Grgit = project.findProperty("grgit") as Grgit  // grgit is added by reckon plugin
    val isClean = grgit.repository.jgit.status().call().isClean
    if (isRelease && !isClean) {
        throw GradleException("Release build will be performed with not clean git tree; aborting.")
    }
}
