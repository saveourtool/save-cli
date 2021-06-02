/**
 * Platform specific utils for logging
 */

package org.cqfn.save.core.logging

import org.cqfn.save.core.config.ResultOutputType

import platform.posix.fdopen
import platform.posix.fflush
import platform.posix.fprintf

@Suppress(
    "WHEN_WITHOUT_ELSE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION")
actual fun logToStream(
    msg: String,
    stream: ResultOutputType) {
    when (stream) {
        ResultOutputType.STDOUT -> processStandardStreams(msg, ResultOutputType.STDOUT)
        ResultOutputType.STDERR -> processStandardStreams(msg, ResultOutputType.STDERR)
        ResultOutputType.FILE -> TODO("Not yet implemented")
    }
}

/**
 * Create proper stream and log a [msg]
 *
 * @param msg a message string
 * @param output output stream (stdout or stderr)
 */
fun processStandardStreams(msg: String, output: ResultOutputType) {
    val stream = when (output) {
        ResultOutputType.STDERR -> fdopen(2, "w")
        else -> fdopen(1, "w")
    }
    fprintf(stream, msg + "\n")
    fflush(stream)
}
