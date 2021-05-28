@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "FILE_NAME_MATCH_CLASS")

package org.cqfn.save.core.utils

/**
 * Atomic values
 */
actual class AtomicInt actual constructor(value: Int) {
    private val atomicInt = kotlin.native.concurrent.AtomicInt(value)

    /**
     * @return value
     */
    actual fun get(): Int = atomicInt.value

    /**
     * @param delta increments the value_ by delta
     * @return the new value
     */
    actual fun addAndGet(delta: Int): Int = atomicInt.addAndGet(delta)
}

actual fun getCurrentOs() = when (Platform.osFamily) {
    OsFamily.LINUX -> CurrentOs.LINUX
    OsFamily.MACOSX -> CurrentOs.MACOS
    OsFamily.WINDOWS -> CurrentOs.WINDOWS
    else -> CurrentOs.UNDEFINED
}
