@file:JvmName("PlatformUtilsJVM")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION")

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.OutputStreamType

actual fun getCurrentOs() = when {
    System.getProperty("os.name").startsWith("Linux", ignoreCase = true) -> CurrentOs.LINUX
    System.getProperty("os.name").startsWith("Mac", ignoreCase = true) -> CurrentOs.MACOS
    System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> CurrentOs.WINDOWS
    else -> CurrentOs.UNDEFINED
}

actual fun writeToConsole(msg: String, outputType: OutputStreamType) {
    when (outputType) {
        OutputStreamType.STDOUT -> System.out.println(msg)
        OutputStreamType.STDERR -> System.err.println(msg)
        else -> return
    }
}

actual typealias AtomicInt = java.util.concurrent.atomic.AtomicInteger
