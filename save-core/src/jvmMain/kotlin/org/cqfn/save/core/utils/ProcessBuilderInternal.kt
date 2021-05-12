package org.cqfn.save.core.utils

import okio.FileSystem
import okio.Path

import java.io.BufferedReader
import java.io.InputStreamReader

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilderInternal actual constructor(
    private val stdoutFile: Path,
    private val stderrFile: Path,
    private val collectStdout: Boolean
) {
    @OptIn(ExperimentalStdlibApi::class)
    actual fun prepareCmd(command: String): String {
        val cmd = buildList {
            if (isCurrentOsWindows()) {
                add("CMD")
                add("/C")
            } else {
                add("sh")
                add("-c")
            }
            add(command)
        }
        return cmd.joinToString()
    }

    actual fun exec(cmd: String): Int {
        val runTime = Runtime.getRuntime()
        val process = runTime.exec(cmd.split(", ").toTypedArray())
        writeDataFromBufferToFile(process, "stdout", stdoutFile)
        writeDataFromBufferToFile(process, "stderr", stderrFile)
        return process.waitFor()
    }

    private fun writeDataFromBufferToFile(
        process: Process,
        stream: String,
        file: Path) {
        if (!collectStdout && stream == "stdout") {
            return
        }
        val br = BufferedReader(
            InputStreamReader(
                if (stream == "stdout") {
                    process.inputStream
                } else {
                    process.errorStream
                }
            )
        )
        val data = br.lineSequence().joinToString("\n")
        FileSystem.SYSTEM.write(file) {
            write(data.encodeToByteArray())
        }
    }
}
