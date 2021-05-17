package org.cqfn.save.core.utils

import kotlinx.serialization.Serializable

/**
 * Supported platforms
 */
@Serializable
@Suppress("MatchingDeclarationName")
enum class CurrentOs {
    LINUX, MACOS, UNDEFINED, WINDOWS
}

/**
 * Checks if the current OS is windows.
 *
 * @return true if current OS is Windows
 */
fun isCurrentOsWindows(): Boolean = (getCurrentOs() == CurrentOs.WINDOWS)
