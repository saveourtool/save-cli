@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.utils

import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logWarn
import okio.Path

@Suppress("MISSING_KDOC_TOP_LEVEL")
actual fun prepareCmd(command: List<String>): String {
    val userCmd = command.joinToString(" ")
    if (userCmd.contains("2>")) {
        logWarn("Found user provided stderr redirection in `$userCmd`. " +
                "SAVE use stderr for internal purpose and will redirect it to the $stderrFile")
    }
    val cmd = "$userCmd >$stdoutFile 2>$stderrFile"
    logDebug("Executing: $cmd")
    return cmd
}

@Suppress("MISSING_KDOC_TOP_LEVEL")
actual fun logAndReturn(
    status: Int,
    redirectTo: Path?): ExecutionResult {
    if (status == -1) {
        fs.deleteRecursively(tmpDir)
        error("Couldn't close the pipe, exit status: $status")
    }
    val stderr = getStderr()
    val stdout = getStdout()
    fs.deleteRecursively(tmpDir)
    if (stderr.isNotEmpty()) {
        logWarn(stderr.joinToString("\n"))
    }
    redirectTo?.let {
        fs.write(redirectTo) {
            write(stdout.joinToString("\n").encodeToByteArray())
        }
    }
        ?: logDebug("Execution output:\n$stdout")
    return ExecutionResult(status, stdout, stderr)
}