@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "FILE_NAME_MATCH_CLASS"
)

package com.saveourtool.save.core.utils

import com.saveourtool.save.core.config.OutputStreamType
import kotlinx.cinterop.ExperimentalForeignApi

import platform.posix.fflush
import platform.posix.fprintf
import platform.posix.stderr
import platform.posix.stdout

@Suppress("USE_DATA_CLASS")
actual class GenericAtomicReference<T> actual constructor(valueToStore: T) {
    private val holder: kotlin.concurrent.AtomicReference<T> = kotlin.concurrent.AtomicReference(valueToStore)
    actual fun get(): T = holder.value
    actual fun set(newValue: T) {
        holder.value = newValue
    }
}

/**
 * Escaping percent symbol in the string in case it is not escaped already
 *
 * @return a new instance of a string with all percent symbols escaped
 */
fun String.escapePercent(): String {
    val stringBuilder = StringBuilder("")
    var percentNum = 0
    this.forEach { char ->
        if (char == '%') {
            // accumulating the number of percents in a raw: a%%%%% -> %: 5
            percentNum++
        } else {
            // normalizing percents and adding them to the string with a non-percent number
            stringBuilder.append("${"%".repeat(if (percentNum % 2 == 0) percentNum else percentNum + 1)}$char")
            percentNum = 0
        }
    }
    // in case only percent symbols are in the string or in the end of the string: a%%%% OR %%%%
    if (percentNum != 0) {
        stringBuilder.append("%".repeat(if (percentNum % 2 == 0) percentNum else percentNum + 1))
    }
    return stringBuilder.toString()
}

actual fun getCurrentOs() = when (Platform.osFamily) {
    OsFamily.LINUX -> CurrentOs.LINUX
    OsFamily.MACOSX -> CurrentOs.MACOS
    OsFamily.WINDOWS -> CurrentOs.WINDOWS
    else -> CurrentOs.UNDEFINED
}

actual fun writeToConsole(msg: String, outputType: OutputStreamType) {
    when (outputType) {
        OutputStreamType.STDOUT -> processStandardStreams(msg, OutputStreamType.STDOUT)
        OutputStreamType.STDERR -> processStandardStreams(msg, OutputStreamType.STDERR)
        else -> return
    }
}

/**
 * Create the proper stream and log a [msg]
 *
 * @param msg a message string
 * @param output output stream (stdout or stderr)
 */
private fun processStandardStreams(msg: String, output: OutputStreamType) {
    @OptIn(ExperimentalForeignApi::class)
    val stream = when (output) {
        OutputStreamType.STDERR -> stderr
        else -> stdout
    }
    @OptIn(ExperimentalForeignApi::class)
    fprintf(stream, msg.escapePercent() + "\n")
    @OptIn(ExperimentalForeignApi::class)
    fflush(stream)
}
