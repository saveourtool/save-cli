/**
 * File Utils for JVM
 */

@file:JvmName("FileUtilsJvm")

package org.cqfn.save.core.files

import org.cqfn.save.core.logging.logDebug

import okio.FileSystem
import okio.Path

import java.nio.file.Files

actual val fs: FileSystem = FileSystem.SYSTEM

/**
 * Delete this directory and all other files and directories in it
 *
 * @param path a path to a directory
 */
actual fun FileSystem.myDeleteRecursively(path: Path) {
    path.toFile().walkBottomUp().forEach {
        logDebug("Attempt to delete file $it")
        Files.delete(it.toPath())
    }
}
