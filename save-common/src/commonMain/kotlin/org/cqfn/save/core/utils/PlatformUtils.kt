/**
 * This file contains platform-dependent utils
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.core.utils

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
