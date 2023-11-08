package com.saveourtool.save.core.utils

import com.saveourtool.save.core.logging.logTrace

import okio.Path
import platform.posix.system

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking

actual fun createProcessBuilderInternal(
    stdoutFile: Path,
    stderrFile: Path,
    useInternalRedirections: Boolean,
): ProcessBuilderInternal = object : ProcessBuilderInternal {
    override fun prepareCmd(command: String) = if (useInternalRedirections) {
        "($command) >$stdoutFile 2>$stderrFile"
    } else {
        command
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun exec(
        cmd: String,
        timeOutMillis: Long
    ): Int {
        var status = -1

        runBlocking {
            val timeOut = async(newSingleThreadContext("timeOut")) {
                delay(timeOutMillis)
                destroy(cmd)
                throw ProcessTimeoutException(timeOutMillis, "Timeout is reached: $timeOutMillis")
            }

            val command = async {
                status = system(cmd)
                timeOut.cancel()
            }
            joinAll(timeOut, command)
        }

        if (status == -1) {
            error("Couldn't execute $cmd, exit status: $status")
        }
        return status
    }

    private fun destroy(cmd: String) {
        val killCmd = if (isCurrentOsWindows()) {
            "taskkill /im \"$cmd\" /f"
        } else {
            "pkill \"$cmd\""
        }
        system(killCmd)
        logTrace("Executed kill command: $killCmd")
    }
}
