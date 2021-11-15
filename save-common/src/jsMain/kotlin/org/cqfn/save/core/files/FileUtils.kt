/**
 * Utility methods for common operations with file system using okio.
 */

@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "MatchingDeclarationName"
)

package org.cqfn.save.core.files

import okio.FileSystem
import okio.Path

@Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
actual val fs: FileSystem by lazy {
    error("Not implemented for JS")
}

actual fun FileSystem.createFile(path: Path, overwrite: Boolean): Path = error("Not implemented for JS")

actual fun FileSystem.myDeleteRecursively(path: Path): Unit = error("Not implemented for JS")
