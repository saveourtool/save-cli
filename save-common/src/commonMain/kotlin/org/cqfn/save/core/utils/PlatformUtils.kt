/**
 * This file contains platform-dependent utils
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.ResultOutputType

import okio.Buffer
import okio.Sink
import okio.Timeout

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
 * A simple okio [Sink] that writes it's input to stdout/stderr
 */
expect class StdStreamsSink(outputType: ResultOutputType) : Sink {
    override fun close()

    override fun flush()

    override fun timeout(): Timeout

    /**
     * Writes a UTF-8 representation of [source] to stdout
     */
    override fun write(source: Buffer, byteCount: Long)
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
expect fun writeToStream(msg: String, outputType: ResultOutputType)
