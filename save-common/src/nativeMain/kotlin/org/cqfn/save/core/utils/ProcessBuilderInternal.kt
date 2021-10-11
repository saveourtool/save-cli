@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.utils

import org.cqfn.save.core.logging.logWarn

import okio.Path
import platform.posix.system

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilderInternal actual constructor(
    private val stdoutFile: Path,
    private val stderrFile: Path,
    private val useInternalRedirections: Boolean
) {
    actual fun prepareCmd(command: String) = if (useInternalRedirections) {
        "($command) >$stdoutFile 2>$stderrFile"
    } else {
        command
    }

    actual fun exec(cmd: String): Int {
        val status = system(cmd)
        if (status == -1) {
            error("Couldn't execute $cmd, exit status: $status")
        }
        return status
    }
}

/**
 * @param command executable command with arguments
 * @param directory where to execute provided command, i.e. `cd [directory]` will be performed before [command] execution
 * @param redirectTo a file where process output and errors should be redirected. If null, output will be returned as [ExecutionResult.stdout] and [ExecutionResult.stderr]
 * @param pb instance that is capable of executing processes
 * @param ms max command execution time
 * @param tests list of tests
 * @return [ExecutionResult] built from process output
 * @throws ProcessExecutionException
 */
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
)
suspend fun execCom(
    command: String,
    directory: String,
    redirectTo: Path?,
    pb: ProcessBuilder,
    ms: Long,
    tests: List<Path>,
): ExecutionResult {
    var execResult: ExecutionResult = ExecutionResult(0, emptyList(), emptyList())

    coroutineScope {
        var endTimer = true
        launch {
            delay(ms)
            if (endTimer) {
                logWarn("The following tests took too long to run and were stopped: $tests")
                throw ProcessExecutionException("Timeout is reached")
            }
        }

        execResult = pb.exec(command, directory, redirectTo)
        endTimer = false
    }

    return execResult
}

/**
 * @param command executable command with arguments
 * @param directory where to execute provided command, i.e. `cd [directory]` will be performed before [command] execution
 * @param redirectTo a file where process output and errors should be redirected. If null, output will be returned as [ExecutionResult.stdout] and [ExecutionResult.stderr]
 * @param pb instance that is capable of executing processes
 * @param ms max command execution time
 * @param tests list of tests
 * @return [ExecutionResult] built from process output
 */
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
)
actual fun exec(
    command: String,
    directory: String,
    redirectTo: Path?,
    pb: ProcessBuilder,
    ms: Long,
    tests: List<Path>,
) = runBlocking {
    return@runBlocking execCom(command, directory, redirectTo, pb, ms, tests)
}
