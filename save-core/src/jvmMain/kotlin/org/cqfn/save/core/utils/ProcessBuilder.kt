package org.cqfn.save.core.utils

import okio.FileSystem
import okio.Path
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logWarn
import java.io.File
import java.lang.ProcessBuilder

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilder {
    private val pb = ProcessBuilder()

    actual fun exec(command: List<String>, redirectTo: Path?): ExecutionResult {
        val fs = FileSystem.SYSTEM
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / this::class.simpleName!!).also {
            fs.createDirectory(it)
        }
        val stdoutFile = tmpDir / "stdout.txt"
        val stderrFile = tmpDir / "stderr.txt"
        logDebug("Created file for stderr: $stderrFile")
        val shell = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) listOf("CMD", "/C") else listOf("sh", "-c")
        val cmd = shell + listOf(command.joinToString(" "))
        logDebug("Executing: ${cmd.joinToString(" ")}")
        val code = pb.command(cmd)
            .let { builder ->
                if (redirectTo != null) {
                    builder.redirectOutput(File(redirectTo.name))
                } else {
                    builder.redirectOutput(File(stdoutFile.toString()))
                }
            }
            .redirectError(File(stderrFile.toString()))
            .start()
            .waitFor()
        val stderr = fs.read(stderrFile) {
            generateSequence { readUtf8Line() }.toList()
        }
        if (stderr.isNotEmpty()) {
            logWarn(stderr.joinToString("\n"))
            fs.deleteRecursively(tmpDir)
            return ExecutionResult(code, emptyList(), stderr)
        }
        val stdout = fs.read(stdoutFile) {
            generateSequence { readUtf8Line() }.toList()
        }
        logDebug("Execution output:\n${stdout.joinToString("\n")}")

        fs.deleteRecursively(tmpDir)
        return ExecutionResult(code, redirectTo?.let { File(it.name).readLines() } ?: stdout, emptyList())
    }
}
