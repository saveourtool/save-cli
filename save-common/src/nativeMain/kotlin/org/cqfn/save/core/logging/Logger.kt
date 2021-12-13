/**
 * Quick & dirty utilities for logging.
 * FixMe: Use proper logging solution once it's available for kotlin/native.
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.core.logging

import org.cqfn.save.core.config.LogType

/**
 *  Logging mode
 */
actual var logType: GenericAtomicReference<LogType> = GenericAtomicReference(LogType.WARN)

@Suppress("USE_DATA_CLASS", "CUSTOM_GETTERS_SETTERS")
actual class GenericAtomicReference<T>(private val toStore: T) {
    @Suppress("IDENTIFIER_LENGTH")
    private val holder: kotlin.native.concurrent.AtomicReference<T> = kotlin.native.concurrent.AtomicReference(toStore)
    actual val value: T
        get() = holder.value
}
