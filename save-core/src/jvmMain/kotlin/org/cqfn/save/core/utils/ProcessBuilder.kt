package org.cqfn.save.core.utils

import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logWarn

import okio.Path

import java.io.File
import java.lang.ProcessBuilder

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilder {
    private val pb = ProcessBuilder()

    actual fun exec(command: List<String>, redirectTo: Path?): ExecutionResult {
        val common = ProcessBuilderInternal()

        logDebug("Created file for stderr: ${common.stderrFile}")
        val shell = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) listOf("CMD", "/C") else listOf("sh", "-c")
        val cmd = shell + listOf(command.joinToString(" "))
        logDebug("Executing: ${cmd.joinToString(" ")}")
        val code = pb.command(cmd)
            .let { builder ->
                redirectTo?.let {
                    builder.redirectOutput(File(redirectTo.name))
                }
                    ?: run {
                        builder.redirectOutput(File(common.stdoutFile.toString()))
                    }
            }
            .redirectError(File(common.stderrFile.toString()))
            .start()
            .waitFor()
        val stdout = common.getStdout()
        val stderr = common.getStderr()
        common.fs.deleteRecursively(common.tmpDir)
        if (stderr.isNotEmpty()) {
            logWarn(stderr.joinToString("\n"))
            return ExecutionResult(code, emptyList(), stderr)
        }
        redirectTo ?: run {
            logDebug("Execution output:\n${stdout.joinToString("\n")}")
        }
        return ExecutionResult(code, redirectTo?.let { File(it.name).readLines() } ?: stdout, emptyList())
    }
}
