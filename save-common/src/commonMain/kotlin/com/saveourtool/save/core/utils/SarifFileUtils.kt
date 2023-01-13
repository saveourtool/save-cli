/**
 * Utility methods to work with SARIF files.
 */

package com.saveourtool.save.core.utils

import com.saveourtool.save.core.files.findAncestorDirContainingFile
import com.saveourtool.save.core.files.fs
import com.saveourtool.save.core.files.parents
import com.saveourtool.save.core.plugin.PluginException

import okio.FileSystem
import okio.Path

/**
 * @return string with trimmed `file://` or `file:///`
 */
fun String.dropFileProtocol() = substringAfter("file://")
    .let {
        // It is a valid format for Windows paths to look like `file:///C:/stuff`
        if (it[0] == '/' && it[2] == ':') it.drop(1) else it
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
fun FileSystem.topmostTestDirectory(path: Path): Path = path.parents().last { parent ->
    list(parent).any { it.name == "save.toml" }
}

/**
 * Calculate the path to sarif file; we expect, that it single for the all tests and located in one of parent directories
 * for evaluated test files
 *
 * @param sarifFileName sarif file name
 * @param anchorTestFilePath anchor file for calculating corresponding sarif file;
 * since .sarif file expected to be the one for all test files, it could be any of test file
 * @return path to sarif
 * @throws PluginException in case of absence of sarif file
 */
fun calculatePathToSarifFile(sarifFileName: String, anchorTestFilePath: Path): Path = fs.findAncestorDirContainingFile(
    anchorTestFilePath, sarifFileName
)?.let {
    it / sarifFileName
} ?: throw PluginException(
    "Could not find SARIF file with expected warnings/fixes for file $anchorTestFilePath. " +
            "Please check if correct `FarningsFormat`/`FixFormat` is set (should be SARIF) and if the file is present and called `$sarifFileName`."
)
