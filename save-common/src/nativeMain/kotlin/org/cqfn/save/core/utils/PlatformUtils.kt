@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "FILE_NAME_MATCH_CLASS"
)

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.OutputStreamType

import platform.posix.fflush
import platform.posix.fprintf
import platform.posix.stderr
import platform.posix.stdout

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

@Suppress("FUNCTION_BOOLEAN_PREFIX")
actual class AtomicBoolean actual constructor(value: Boolean) {
    private val atomicBoolean = kotlin.native.concurrent.AtomicReference(value)

    /**
     * @return value
     */
    actual fun get(): Boolean = atomicBoolean.value

    /**
     * @param expect expected value
     * @param update updated value
     * @return the result of the comparison
     */
    actual fun compareAndSet(expect: Boolean, update: Boolean): Boolean = atomicBoolean.compareAndSet(expect, update)
}

@Suppress("USE_DATA_CLASS")
actual class GenericAtomicReference<T> actual constructor(valueToStore: T) {
    private val holder: kotlin.native.concurrent.AtomicReference<T> = kotlin.native.concurrent.AtomicReference(valueToStore)
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
fun processStandardStreams(msg: String, output: OutputStreamType) {
    val stream = when (output) {
        OutputStreamType.STDERR -> stderr
        else -> stdout
    }
    fprintf(stream, msg.escapePercent() + "\n")
    fflush(stream)
}
