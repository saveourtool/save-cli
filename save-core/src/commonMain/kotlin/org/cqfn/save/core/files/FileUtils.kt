/**
 * Utility methods for common operations with file system using okio.
 */

package org.cqfn.save.core.files

import okio.FileSystem
import okio.Path

/**
 * Find all descendant files in the directory denoted by [this] [Path], that match [condition].
 *
 * @param condition a condition to match
 * @return a list of files
 */
fun Path.findAllFilesMatching(condition: (Path) -> Boolean): List<Path> = FileSystem.SYSTEM.list(this).flatMap {
    when {
        FileSystem.SYSTEM.metadata(it).isDirectory -> it.findAllFilesMatching(condition)
        condition.invoke(it) -> listOf(it)
        else -> emptyList()
    }
}

/**
 * @param condition a condition to match
 * @return a matching child file or null
 */
fun Path.findChildByOrNull(condition: (Path) -> Boolean): Path? {
    require(FileSystem.SYSTEM.metadata(this).isDirectory)
    return FileSystem.SYSTEM.list(this).firstOrNull(condition)
}

/**
 * @return a [Sequence] of file parent directories
 */
fun Path.parents(): Sequence<Path> = generateSequence(parent) { it.parent }

/**
 * @param condition a condition to match
 */
fun Path.findAllParentsMatching(condition: (Path) -> Boolean) = parents().filter(condition)
