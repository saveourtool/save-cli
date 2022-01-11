/**
 * Utility methods to work with SARIF files.
 */

package org.cqfn.save.plugin.warn.sarif

import org.cqfn.save.core.files.parents

import okio.FileSystem
import okio.Path

/**
 * Find a file in any of parent directories and return this directory
 *
 * @param path path for which ancestors should be checked
 * @param fileName a name of the file that will be searched for
 * @return a path to one of parent directories or null if no directory contains [fileName]
 */
fun FileSystem.findAncestorDirContainingFile(path: Path, fileName: String): Path? = path.parents().firstOrNull { parent ->
    metadata(parent).isDirectory && list(parent).any {
        it.name == fileName
    }
}

/**
 * Make all paths in [this] collection relative to [root]
 *
 * @param root a common root for files in [this]
 * @return a list of relative paths
 */
fun List<Path>.adjustToCommonRoot(root: Path) = map {
    it.relativeTo(root).normalized()
}

/**
 * Find the last parent directory containing save.toml.
 *
 * @param path a path to start the search
 * @return one of parent directories
 */
internal fun FileSystem.topmostTestDirectory(path: Path): Path = path.parents().last { parent ->
    list(parent).any { it.name == "save.toml" }
}
