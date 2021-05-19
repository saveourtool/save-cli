/**
 * Utility methods for common operations with file system using okio.
 */

package org.cqfn.save.core.files

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

val testResourceFilePattern = Regex(".*Test\\.\\w+")

/**
 * Find all descendant files in the directory denoted by [this] [Path], that match [condition].
 * This method uses BFS, so files will appear in the returned list in order of directory levels.
 *
 * @param condition a condition to match
 * @return a list of files
 */
fun Path.findAllFilesMatching(condition: (Path) -> Boolean): List<List<Path>> = FileSystem.SYSTEM.list(this)
    .partition { FileSystem.SYSTEM.metadata(it).isDirectory }
    .let { (directories, files) ->
        listOf(files.filter(condition)) + directories.flatMap {
            it.findAllFilesMatching(condition)
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

/**
 * Create file in [this] [FileSystem], denoted by path [pathString]
 *
 * @param pathString path to a new file
 * @return [Path] denoting the created file
 */
fun FileSystem.createFile(pathString: String): Path = createFile(pathString.toPath())

/**
 * Create file in [this] [FileSystem], denoted by [Path] [path]
 *
 * @param path path to a new file
 * @return [path]
 */
fun FileSystem.createFile(path: Path): Path {
    sink(path).close()
    return path
}

/**
 * @param path a path to a file
 * @return list of strings from the file
 */
fun FileSystem.readLines(path: Path): List<String> = this.read(path) {
    generateSequence { readUtf8Line() }.toList()
}

/**
 * Returns a sequence of underlying directories, filtering on every level by [directoryPredicate].
 * Example:
 * ```
 * | directory1
 * |_ file1
 * | | directory11
 * | |_ file2
 * | |_ directory21
 * | | directory12
 * | | directory13
 * | |_ directory23
 * | | |_ file33
 * ```
 * If predicate returns `true` when `Path` is a directory which contains only other directories, then `directory11` will be filtered, as well as
 * `directory21`, which is empty, but is a descendant of already filtered out one, and `directory 23`, which is not empty.
 * So, the result would be `sequence(directory12, directory13)`
 *
 * @param directoryPredicate a predicate to match directories
 * @return a sequence of matching directories
 */
fun Path.findDescendantDirectoriesBy(directoryPredicate: (Path) -> Boolean): Sequence<Path> =
    sequence {
        yield(this@findDescendantDirectoriesBy)
        FileSystem.SYSTEM.list(this@findDescendantDirectoriesBy)
            .asSequence()
            .filter { FileSystem.SYSTEM.metadata(it).isDirectory }
            .filter(directoryPredicate)
            .flatMap { it.findDescendantDirectoriesBy(directoryPredicate) }
            .let { yieldAll(it) }
    }
