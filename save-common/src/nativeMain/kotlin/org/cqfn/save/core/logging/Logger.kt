package org.cqfn.save.core.logging

import org.cqfn.save.core.config.ResultOutputType
import platform.posix.fdopen
import platform.posix.fprintf
import platform.posix.fflush

actual fun logToStream(level: String, msg: String, output: ResultOutputType) {
    when (output) {
        ResultOutputType.STDOUT -> {
            processStandardStreams(msg, ResultOutputType.STDOUT)
        }
        ResultOutputType.STDERR -> {
            processStandardStreams(msg, ResultOutputType.STDERR)
        }
        ResultOutputType.FILE -> {

        }
    }
}

fun processStandardStreams(msg: String, output: ResultOutputType) {
    val stream = when (output) {
        ResultOutputType.STDERR -> fdopen(2, "w")
        else -> fdopen(1, "w")
    }
    fprintf(stream, msg + "\n")
    fflush(stream)
}