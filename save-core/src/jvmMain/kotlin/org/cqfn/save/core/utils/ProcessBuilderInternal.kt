package org.cqfn.save.core.utils

import okio.FileSystem
import okio.Path
import org.cqfn.save.core.logging.logDebug
import java.io.BufferedReader
import java.io.InputStreamReader

import java.util.stream.Collectors

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilderInternal actual constructor(
    private val stdoutFile: Path, private val stderrFile: Path,
    private val collectStdout: Boolean
) {
    actual fun prepareCmd(command: String): String {
        val shell = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) arrayOf("CMD", "/C") else arrayOf("sh", "-c")
        val cmd = arrayOf(*shell, command)
        return cmd.joinToString()
    }

    actual fun exec(cmd: String): Int {
        logDebug("Executing: $cmd")
        val runTime = Runtime.getRuntime()
        val process = runTime.exec(cmd.split(", ").toTypedArray())
        writeDataFromBufferToFile(process, "stdout", stdoutFile)
        writeDataFromBufferToFile(process, "stderr", stderrFile)
        // TODO: Does waitFor() == -1 responsible for errors like in posix?
        return process.waitFor()
    }

    private fun writeDataFromBufferToFile(process: Process, stream: String, file: Path) {
        if (!collectStdout && stream == "stdout") {
            return
        }
        val br = if (stream == "stdout") {
            BufferedReader(InputStreamReader(process.inputStream))
        }
        else {
            BufferedReader(InputStreamReader(process.errorStream))
        }
        val data = br.lines().collect(Collectors.joining("\n"))
        FileSystem.SYSTEM.write(file) {
            write(data.encodeToByteArray())
        }
    }
}

