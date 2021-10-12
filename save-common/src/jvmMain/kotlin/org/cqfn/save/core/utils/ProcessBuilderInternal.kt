package org.cqfn.save.core.utils

import org.cqfn.save.core.logging.logWarn

import okio.FileSystem
import okio.Path

import java.io.BufferedReader
import java.io.InputStreamReader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext

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

/**
 * @param command executable command with arguments
 * @param directory where to execute provided command, i.e. `cd [directory]` will be performed before [command] execution
 * @param redirectTo a file where process output and errors should be redirected. If null, output will be returned as [ExecutionResult.stdout] and [ExecutionResult.stderr]
 * @param pb instance that is capable of executing processes
 * @param timeOutMillis max command execution time
 * @param tests list of tests
 * @return [ExecutionResult] built from process output
 * @throws ProcessExecutionException
 */
@OptIn(ObsoleteCoroutinesApi::class)
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
)
actual fun exec(
    command: String,
    directory: String,
    redirectTo: Path?,
    pb: ProcessBuilder,
    timeOutMillis: Long,
    tests: List<Path>,
): ExecutionResult {
    val processContext = newFixedThreadPoolContext(2, "timeOut")
    val endTimer = AtomicBoolean(true)

    CoroutineScope(processContext).launch {
        delay(timeOutMillis)
        if (endTimer.get()) {
            logWarn("The following tests took too long to run and were stopped: $tests")
            throw ProcessExecutionException("Timeout is reached")
        }
    }

    val execResult: ExecutionResult = pb.exec(command, directory, redirectTo)
    endTimer.compareAndSet(true, false)

    return execResult
}
