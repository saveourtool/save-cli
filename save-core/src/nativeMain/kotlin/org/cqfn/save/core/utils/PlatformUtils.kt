package org.cqfn.save.core.utils

actual fun isCurrentOsWindows(): Boolean {
    return Platform.osFamily == OsFamily.WINDOWS
}