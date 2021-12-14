@file:JvmName("PlatformUtilsJVM")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "FILE_NAME_MATCH_CLASS"
)

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.OutputStreamType

actual typealias AtomicInt = java.util.concurrent.atomic.AtomicInteger

actual typealias AtomicBoolean = java.util.concurrent.atomic.AtomicBoolean

@Suppress("USE_DATA_CLASS")
actual class GenericAtomicReference<T> actual constructor(valueToStore: T) {
    private var value: T = valueToStore
    actual fun get(): T = value
    actual fun set(newValue: T) {
        value = newValue
    }
}

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
