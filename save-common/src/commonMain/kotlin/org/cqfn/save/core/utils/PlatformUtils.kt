/**
 * This file contains platform-dependent utils
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.ResultOutputType

/**
 * Supported platforms
 */
enum class CurrentOs {
    LINUX, MACOS, UNDEFINED, WINDOWS
}

/**
 * Atomic values
 */
expect class AtomicInt(value: Int) {
    /**
     * @return value
     */
    fun get(): Int

    /**
     * @param delta increments the value_ by delta
     * @return the new value
     */
    fun addAndGet(delta: Int): Int
}

/**
 * Get type of current OS
 *
 * @return type of current OS
 */
expect fun getCurrentOs(): CurrentOs

/**
 * Checks if the current OS is windows.
 *
 * @return true if current OS is Windows
 */
fun isCurrentOsWindows(): Boolean = (getCurrentOs() == CurrentOs.WINDOWS)

/**
 * Platform specific writer
 *
 * @param msg a message string
 * @param outputType output stream (file, stdout, stderr)
 */
@Suppress("WHEN_WITHOUT_ELSE")
fun writeToStream(msg: String, outputType: ResultOutputType) {
    when (outputType) {
        ResultOutputType.STDOUT, ResultOutputType.STDERR -> writeToConsole(msg, outputType)
        ResultOutputType.FILE -> TODO("Not yet implemented")
    }
}

/**
 * Platform specific writer to console
 *
 * @param msg a message string
 * @param outputType output stream: stdout or stderr
 */
expect fun writeToConsole(msg: String, outputType: ResultOutputType)
