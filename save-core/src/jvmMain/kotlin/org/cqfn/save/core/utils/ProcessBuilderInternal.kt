package org.cqfn.save.core.utils

import okio.Path
import org.cqfn.save.core.logging.logDebug

import java.lang.ProcessBuilder

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilderInternal {
    private val pb = ProcessBuilder()

    actual fun prepareCmd(command: String, collectStdout: Boolean, stdoutFile: Path, stderrFile: Path): String {
        val shell = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) listOf("CMD", "/C") else listOf("sh", "-c")
        val cmd = if (collectStdout) {
            shell + listOf("\"$command") + listOf(" >$stdoutFile 2>$stderrFile\"")
        } else {
            shell + listOf("\"$command") + listOf(" 2>$stderrFile\"")
        }
        return cmd.joinToString(" ")
    }

    actual fun exec(cmd: String): Int {
        logDebug("Executing: $cmd")
        // TODO: Does status == -1 responsible for errors like in posix?
        val status = pb.command((cmd.split(" ")))
            .start()
            .waitFor()
        return status
    }
}
