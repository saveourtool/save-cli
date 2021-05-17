@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION")

package org.cqfn.save.core.utils

actual fun getCurrentOs() = when (Platform.osFamily) {
    OsFamily.LINUX -> CurrentOs.LINUX
    OsFamily.MACOSX -> CurrentOs.MACOS
    OsFamily.WINDOWS -> CurrentOs.WINDOWS
    else -> CurrentOs.UNDEFINED
}
