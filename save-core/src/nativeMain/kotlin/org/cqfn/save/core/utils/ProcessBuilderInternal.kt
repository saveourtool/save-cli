@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.utils

import okio.Path
import org.cqfn.save.core.logging.logDebug
import platform.posix.system

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilderInternal {
    actual fun prepareCmd(command: String, collectStdout: Boolean, stdoutFile: Path, stderrFile: Path): String {
        val cmd = if (collectStdout) {
            "$command >$stdoutFile 2>$stderrFile"
        } else {
            "$command 2>$stderrFile"
        }
        return cmd
    }

    actual fun exec(cmd: String): Int {
        logDebug("Executing: $cmd")
        val status = system(cmd)
        //if (status == -1) {
            //fs.deleteRecursively(tmpDir)
            //error("Couldn't execute $cmd, exit status: $status")
        //}
        return status
    }
}
