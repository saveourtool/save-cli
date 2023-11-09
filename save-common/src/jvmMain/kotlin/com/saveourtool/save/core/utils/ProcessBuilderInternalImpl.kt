package com.saveourtool.save.core.utils

import com.saveourtool.save.core.files.fs
import okio.Path

import java.io.BufferedReader
import java.io.InputStreamReader

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking

private class ProcessBuilderInternalImpl(
    private val stdoutFile: Path,
    private val stderrFile: Path,
    private val useInternalRedirections: Boolean,
) : ProcessBuilderInternal {
    override fun prepareCmd(command: String): String {
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

    @Suppress("UnsafeCallOnNullableType")
    override fun exec(
        cmd: String,
        timeOutMillis: Long,
    ): Int {
        var status = -1
        runBlocking {
            val processContext = newFixedThreadPoolContext(2, "timeOut")

            val runTime = Runtime.getRuntime()
            var process: Process? = null
            val job = launch(processContext) {
                val timeOut = launch {
                    delay(timeOutMillis)
                    process?.destroy()
                    throw ProcessTimeoutException(timeOutMillis, "Timeout is reached: $timeOutMillis")
                }
                launch {
                    process = runTime.exec(cmd.split(", ").toTypedArray())
                    writeDataFromBufferToFile(process!!, "stdout", stdoutFile)
                    writeDataFromBufferToFile(process!!, "stderr", stderrFile)
                    status = process!!.waitFor()
                    timeOut.cancel()
                }
            }
            job.join()
        }
        return status
    }

    private fun writeDataFromBufferToFile(
        process: Process,
        stream: String,
        file: Path,
    ) {
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
        val data = br.useLines {
            it.joinToString("\n")
        }
        fs.write(file) {
            write(data.encodeToByteArray())
        }
    }
}

actual fun createProcessBuilderInternal(
    stdoutFile: Path,
    stderrFile: Path,
    useInternalRedirections: Boolean,
): ProcessBuilderInternal = ProcessBuilderInternalImpl(stdoutFile, stderrFile, useInternalRedirections)
