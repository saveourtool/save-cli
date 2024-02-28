@file:JvmName("PlatformUtilsJVM")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "FILE_NAME_MATCH_CLASS",
    "MatchingDeclarationName",
)

package com.saveourtool.save.core.utils

import com.saveourtool.save.core.config.OutputStreamType

actual fun <T> createGenericAtomicReference(valueToStore: T): GenericAtomicReference<T> =
        object : GenericAtomicReference<T> {
            private val holder = java.util.concurrent.atomic.AtomicReference(valueToStore)
            override fun get(): T = holder.get()
            override fun set(newValue: T) {
                holder.set(newValue)
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
