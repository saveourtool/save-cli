@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "KDOC_NO_EMPTY_TAGS",
    "MISSING_KDOC_ON_FUNCTION",
    "FILE_NAME_MATCH_CLASS")

package org.cqfn.save.core.utils

import okio.Path

actual class ProcessBuilderInternal actual constructor(
    stdoutFile: Path,
    stderrFile: Path,
    useInternalRedirections: Boolean) {
    actual fun prepareCmd(command: String): String = error("Not implemented for JS")

    actual fun exec(cmd: String): Int = error("Not implemented for JS")
}
