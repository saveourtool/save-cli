/**
 * Utility methods for common operations with file system using okio.
 */

@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "MatchingDeclarationName",
)

package com.saveourtool.save.core.files

import okio.FileSystem
import okio.Path

@Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
actual val fs: FileSystem by lazy {
    error("Not implemented for JS")
}

actual fun FileSystem.myDeleteRecursively(path: Path): Unit = error("Not implemented for JS")

actual fun getWorkingDirectory(): Path = error("Not implemented for JS")
