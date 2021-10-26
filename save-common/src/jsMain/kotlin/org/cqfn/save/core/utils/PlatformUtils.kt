@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "KDOC_NO_EMPTY_TAGS",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "FILE_NAME_MATCH_CLASS")

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.OutputStreamType

actual class AtomicInt actual constructor(value: Int) {
    actual fun get(): Int = TODO()
    actual fun addAndGet(delta: Int): Int = TODO()
}

actual fun getCurrentOs(): CurrentOs = error("OS is not defined for JS")

actual fun writeToConsole(msg: String, outputType: OutputStreamType) {
    error("This method is not defined for JS")
}
