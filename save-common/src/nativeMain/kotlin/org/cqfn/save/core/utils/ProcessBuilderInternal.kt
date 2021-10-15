@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.utils

import okio.Path
import platform.posix.system

import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineExceptionHandler

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
        timeOutMillis: Long
    ): Int {
        var status = -1

        runBlocking {

            val handler = CoroutineExceptionHandler { _, exception ->
                println("CoroutineExceptionHandler got $exception")
            }

            val endTimer = AtomicBoolean(true)
            val a1 = async(handler) {
                delay(timeOutMillis)
                if (endTimer.get()) {
                    runCatching {
                        destroy(cmd)
                        throw ProcessTimeoutException(timeOutMillis, "Timeout is reached")
                    }
                }
            }

            val a2 = async(handler) {
                system(cmd).also {
                    endTimer.compareAndSet(true, false)
                }
            }
            try {
                joinAll(a1, a2)
            } catch (e: Exception) {
                throw ProcessTimeoutException(timeOutMillis, "Timeout is reached")
            }
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
