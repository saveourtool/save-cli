package org.cqfn.save.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.ObsoleteCoroutinesApi

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
    private val useInternalRedirections: Boolean
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

    @OptIn(ObsoleteCoroutinesApi::class)
    actual fun exec(
        cmd: String,
        timeOutMillis: Long,
    ): Int {
        val processContext = newFixedThreadPoolContext(2, "timeOut")
        val endTimer = AtomicBoolean(true)

        val runTime = Runtime.getRuntime()
        val process = runTime.exec(cmd.split(", ").toTypedArray())
        writeDataFromBufferToFile(process, "stdout", stdoutFile)
        writeDataFromBufferToFile(process, "stderr", stderrFile)

        CoroutineScope(processContext).launch {
            delay(timeOutMillis)
            if (endTimer.get()) {
                process.destroy()
                throw ProcessTimeoutException(timeOutMillis, "Timeout is reached")
            }
        }
        return process.waitFor()
    }

    private fun writeDataFromBufferToFile(
        process: Process,
        stream: String,
        file: Path) {
        if (!useInternalRedirections) {
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
