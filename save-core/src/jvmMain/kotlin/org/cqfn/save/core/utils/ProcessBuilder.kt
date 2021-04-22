@file:JvmName("ProcessBuilderJvm")

package org.cqfn.save.core.utils

import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logWarn

import okio.Path

import java.lang.ProcessBuilder

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilderInternal {
    private val pb = ProcessBuilder()

    actual fun exec(cmd: String): Int {
        val code = pb.command((cmd.split(" ")))
            .start()
            .waitFor()
        return code
    }
}

@Suppress("MISSING_KDOC_TOP_LEVEL")
actual fun prepareCmd(command: List<String>): String {
    val userCmd = command.joinToString(" ")
    if (userCmd.contains("2>")) {
        logWarn("Found user provided stderr redirection in `$userCmd`. " +
                "SAVE use stderr for internal purpose and will redirect it to the $stderrFile")
    }
    val shell = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) listOf("CMD", "/C") else listOf("sh", "-c")
    val cmd = shell + listOf(userCmd) + listOf(" >$stdoutFile 2>$stderrFile")
    logDebug("Executing: $cmd")
    return cmd.joinToString(" ")
}

@Suppress("MISSING_KDOC_TOP_LEVEL")
actual fun logAndReturn(
    status: Int,
    redirectTo: Path?): ExecutionResult {
    val stdout = getStdout()
    val stderr = getStderr()
    fs.deleteRecursively(tmpDir)
    if (stderr.isNotEmpty()) {
        logWarn(stderr.joinToString("\n"))
    }
    redirectTo?.let {
        fs.write(redirectTo) {
            write(stdout.joinToString("\n").encodeToByteArray())
        }
    } ?: logDebug("Execution output:\n$stdout")
    return ExecutionResult(status, stdout, stderr)
}
