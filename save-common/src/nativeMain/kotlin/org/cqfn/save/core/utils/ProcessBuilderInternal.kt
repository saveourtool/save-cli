@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.utils

import okio.Path
import platform.posix.system

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilderInternal actual constructor(
    private val stdoutFile: Path,
    private val stderrFile: Path,
    private val collectStdout: Boolean
) {
    actual fun prepareCmd(command: String) = if (collectStdout) {
        "($command) >$stdoutFile 2>$stderrFile"
    } else {
        "($command) 2>$stderrFile"
    }

    actual fun exec(cmd: String): Int {
        val status = system(cmd)
        if (status == -1) {
            error("Couldn't execute $cmd, exit status: $status")
        }
        return status
    }
}
