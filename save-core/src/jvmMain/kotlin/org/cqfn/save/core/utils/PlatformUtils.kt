package org.cqfn.save.core.utils

actual fun isCurrentOsWindows(): Boolean {
    return System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
}
