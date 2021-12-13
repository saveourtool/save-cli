/**
 * Quick & dirty utilities for logging.
 * FixMe: Use proper logging solution once it's available for kotlin/native.
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")
@file:JvmName("LoggerJVM")

package org.cqfn.save.core.logging

import org.cqfn.save.core.config.LogType

import kotlin.jvm.JvmName

/**
 *  Logging mode
 */
actual var logType: GenericAtomicReference<LogType> = GenericAtomicReference(LogType.WARN)

@Suppress("USE_DATA_CLASS", "CUSTOM_GETTERS_SETTERS")
actual class GenericAtomicReference<T>(private val toStore: T) {
    actual val value: T
        get() = toStore
}
