@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "FILE_NAME_MATCH_CLASS")

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.ResultOutputType

import platform.posix.fdopen
import platform.posix.fflush
import platform.posix.fprintf

/**
 * Atomic values
 */
actual class AtomicInt actual constructor(value: Int) {
    private val atomicInt = kotlin.native.concurrent.AtomicInt(value)

    /**
     * @return value
     */
    actual fun get(): Int = atomicInt.value

    /**
     * @param delta increments the value_ by delta
     * @return the new value
     */
    actual fun addAndGet(delta: Int): Int = atomicInt.addAndGet(delta)
}

actual fun getCurrentOs() = when (Platform.osFamily) {
    OsFamily.LINUX -> CurrentOs.LINUX
    OsFamily.MACOSX -> CurrentOs.MACOS
    OsFamily.WINDOWS -> CurrentOs.WINDOWS
    else -> CurrentOs.UNDEFINED
}

actual fun writeToConsole(msg: String, outputType: ResultOutputType) {
    when (outputType) {
        ResultOutputType.STDOUT -> processStandardStreams(msg, ResultOutputType.STDOUT)
        ResultOutputType.STDERR -> processStandardStreams(msg, ResultOutputType.STDERR)
        else -> return
    }
}

/**
 * Create the proper stream and log a [msg]
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
