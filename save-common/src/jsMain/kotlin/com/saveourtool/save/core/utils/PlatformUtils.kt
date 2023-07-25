@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "KDOC_NO_EMPTY_TAGS",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "FILE_NAME_MATCH_CLASS",
    "MatchingDeclarationName",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "FUNCTION_BOOLEAN_PREFIX",
)

package com.saveourtool.save.core.utils

import com.saveourtool.save.core.config.OutputStreamType

@Suppress("USE_DATA_CLASS")
actual class GenericAtomicReference<T> actual constructor(valueToStore: T) {
    private var value: T = valueToStore
    actual fun get(): T = value
    actual fun set(newValue: T) {
        value = newValue
    }
}

actual fun getCurrentOs(): CurrentOs = error("Not implemented for JS")

actual fun writeToConsole(msg: String, outputType: OutputStreamType) {
    error("Not implemented for JS")
}
