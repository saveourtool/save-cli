@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.utils

import org.cqfn.save.core.logging.logWarn

import okio.Path
import platform.posix.system

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

    actual fun exec(
        cmd: String,
        timeOutMillis: Long,
        tests: List<Path>): Int {
        var status = -1

        runBlocking {
            val endTimer = AtomicBoolean(true)
            launch {
                delay(timeOutMillis)
                if (endTimer.get()) {
                    logWarn("The following tests took too long to run and were stopped: $tests")
                    destroy(cmd)
                    throw ProcessExecutionException("Timeout is reached")
                }
            }

            status = system(cmd)
            endTimer.compareAndSet(true, false)
        }

        if (status == -1) {
            error("Couldn't execute $cmd, exit status: $status")
        }
        return status
    }

    private fun destroy(cmd: String) {
        val killCmd = "pkill $cmd"
        system(killCmd)
    }
}
