@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.utils

import okio.FileSystem
import okio.Path
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen

import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logWarn

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilder {
    actual fun exec(command: List<String>, redirectTo: Path?): ExecutionResult {
        val fs = FileSystem.SYSTEM
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / this::class.simpleName!!).also {
            fs.createDirectory(it)
        }
        val stderrFile = tmpDir / "stderr.txt"
        fs.createFile(stderrFile)
        logDebug("Created file for stderr: $stderrFile")
        val cmd = command.joinToString(" ") + " 2>$stderrFile"
        logDebug("Executing: $cmd")
        val pd = popen(cmd, "r")
            ?: error("Pipe error. Couldn't execute command: `$command`")
        val stdout = buildString {
            val buffer = ByteArray(4096)
            while (fgets(buffer.refTo(0), buffer.size, pd) != null) {
                append(buffer.toKString())
            }
        }

        val status = pclose(pd)
        if (status == -1) {
            fs.deleteRecursively(tmpDir)
            error("Couldn't close the pipe, exit status: $status")
        }
        val stderr = fs.read(stderrFile) {
            generateSequence { readUtf8Line() }.toList()
        }
        fs.deleteRecursively(tmpDir)
        if (stderr.isNotEmpty()) {
            logWarn(stderr.joinToString("\n"))
            return ExecutionResult(status, emptyList(), stderr)
        }
        if (redirectTo != null) {
            fs.write(redirectTo) {
                write(stdout.encodeToByteArray())
            }
        } else {
            logDebug("Execution output:\n${stdout}")
        }
        return ExecutionResult(0, stdout.split("\n"), emptyList())
    }
}
