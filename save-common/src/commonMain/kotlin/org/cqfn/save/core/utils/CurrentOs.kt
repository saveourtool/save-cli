package org.cqfn.save.core.utils

/**
 * Supported platforms
 */
enum class CurrentOs {
    LINUX, MACOS, UNDEFINED, WINDOWS
}

/**
 * Checks if the current OS is windows.
 *
 * @return true if current OS is Windows
 */
fun isCurrentOsWindows(): Boolean = (getCurrentOs() == CurrentOs.WINDOWS)
