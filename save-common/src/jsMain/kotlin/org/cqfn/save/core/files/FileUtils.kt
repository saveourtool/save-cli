/**
 * Utility methods for common operations with file system using okio.
 */

@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "MatchingDeclarationName"
)

package org.cqfn.save.core.files

import okio.FileSystem

actual val fs: FileSystem by lazy {
    error("Not implemented for JS")
}
