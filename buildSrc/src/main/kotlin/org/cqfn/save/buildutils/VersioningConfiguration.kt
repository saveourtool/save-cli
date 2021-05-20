/**
 * Version configuration file.
 */

@file:Suppress("UNUSED_IMPORT")

package org.cqfn.save.buildutils

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * Configures how project version is determined.
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
    if (isRelease) {
        val grgit = project.findProperty("grgit") as Grgit  // grgit property is added by reckon plugin
        val status = grgit.repository.jgit.status().call()
        if (!status.isClean) {
            throw GradleException("Release build will be performed with not clean git tree; aborting. " +
                    "Untracked files: ${status.untracked}, uncommitted changes: ${status.uncommittedChanges}")
        }
    }
}
