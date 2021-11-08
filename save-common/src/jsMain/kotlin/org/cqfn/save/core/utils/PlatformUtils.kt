@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "KDOC_NO_EMPTY_TAGS",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "FILE_NAME_MATCH_CLASS",
    "MatchingDeclarationName",
)

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.OutputStreamType

actual class AtomicInt actual constructor(value: Int) {
    actual fun get(): Int = error("Not implemented for JS")
    actual fun addAndGet(delta: Int): Int = error("Not implemented for JS")
}

actual class AtomicBoolean actual constructor(value: Boolean) {
    actual fun get(): Boolean = error("Not implemented for JS")
    actual fun compareAndSet(expect: Boolean, update: Boolean): Boolean = error("Not implemented for JS")
}

actual fun getCurrentOs(): CurrentOs = error("Not implemented for JS")

actual fun writeToConsole(msg: String, outputType: OutputStreamType) {
    error("Not implemented for JS")
}
