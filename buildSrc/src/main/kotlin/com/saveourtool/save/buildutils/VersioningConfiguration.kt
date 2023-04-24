/**
 * Version configuration file.
 */

package com.saveourtool.save.buildutils

import org.ajoberstar.reckon.core.Scope
import org.ajoberstar.reckon.core.VersionTagParser
import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import java.util.Optional

/**
 * Configures how project version is determined.
 *
 * @throws GradleException if there was an attempt to run release build with dirty working tree
 */
fun Project.configureVersioning() {
    apply<ReckonPlugin>()

    val isSnapshot = hasProperty("reckon.stage") && property("reckon.stage") == "snapshot"
    configure<ReckonExtension> {
        setDefaultInferredScope(Scope.MINOR.name)
        setScopeCalc(calcScopeFromProp())
        if (isSnapshot) {
            // we should build snapshots only for snapshot publishing, so it requires explicit parameter
            snapshots()
            setStageCalc(calcStageFromProp())
            fixForSnapshot()
        } else {
            stages("alpha", "rc", "final")
            setStageCalc(calcStageFromProp())
        }
    }

    // to activate release, provide `-Prelease` or `-Prelease=true`. To deactivate, either omit the property, or set `-Prelease=false`.
    val isRelease = hasProperty("release") && (property("release") as String != "false")
    if (isRelease) {
        failOnUncleanTree()
    }
}

private fun Project.failOnUncleanTree() {
    val status = FileRepositoryBuilder()
        .findGitDir(project.rootDir)
        .setup()
        .let(::FileRepository)
        .let(::Git)
        .status()
        .call()
    if (!status.isClean) {
        throw GradleException("Release build will be performed with not clean git tree; aborting. " +
                "Untracked files: ${status.untracked}, uncommitted changes: ${status.uncommittedChanges}")
    }
}

/**
 * A terrible hack to remove all pre-release tags. Because in semver `0.1.0-SNAPSHOT` < `0.1.0-alpha`, in snapshot mode
 * we remove tags like `0.1.0-alpha`, and then reckoned version will still be `0.1.0-SNAPSHOT` and it will be compliant.
 */
private fun ReckonExtension.fixForSnapshot() {
    setTagParser { tagName ->
        Optional.of(tagName)
            .filter { it.matches(Regex("""^v\d+\.\d+\.\d+$""")) }
            .flatMap { VersionTagParser.getDefault().parse(it) }
    }
}
