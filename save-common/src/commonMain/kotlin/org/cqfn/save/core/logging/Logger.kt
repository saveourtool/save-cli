/**
 * Quick & dirty utilities for logging.
 * FixMe: Use proper logging solution once it's available for kotlin/native.
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.core.logging

import org.cqfn.save.core.config.LogType
import org.cqfn.save.core.config.OutputStreamType
import org.cqfn.save.core.utils.writeToStream

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 *  Logging mode
 */
expect var logType: GenericAtomicReference<LogType>

/**
 * Whether to add time stamps to log messages
 */
var isTimeStampsEnabled: Boolean = false

/**
 *  Class that holds value and shares atomic reference to the value (native only)
 */
@Suppress("USE_DATA_CLASS")
expect class GenericAtomicReference<T> {
    /**
     * Stored value
     */
    val value: T
}

/**
 * Log a message to the [stream] with timestamp and specific [level]
 *
 * @param level log level
 * @param msg a message string
 * @param stream output stream (file, stdout, stderr)
 */
fun logMessage(
    level: String,
    msg: String,
    stream: OutputStreamType
) {
    val currentTime = if (isTimeStampsEnabled) {
        val currentTimeInstance = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        " ${currentTimeInstance.date} ${currentTimeInstance.hour}:${currentTimeInstance.minute}:${currentTimeInstance.second}"
    } else {
        ""
    }
    writeToStream("[$level]$currentTime: $msg", stream)
}

/**
 * Log a message with info level
 *
 * @param msg a message string
 */
fun logInfo(msg: String) {
    logMessage("INFO", msg, OutputStreamType.STDOUT)
}

/**
 * Log a message with error level
 *
 * @param msg a message string
 */
fun logError(msg: String) {
    logMessage("ERROR", msg, OutputStreamType.STDERR)
}

/**
 * Log a message with warn level
 *
 * @param msg a message string
 */
fun logWarn(msg: String) {
    if (logType.value == LogType.WARN || logType.value == LogType.DEBUG || logType.value == LogType.ALL) {
        logMessage("WARN", msg, OutputStreamType.STDERR)
    }
}

/**
 * Log a message with debug level
 *
 * @param msg a message string
 */
fun logDebug(msg: String) {
    if (logType.value == LogType.DEBUG || logType.value == LogType.ALL) {
        logMessage("DEBUG", msg, OutputStreamType.STDOUT)
    }
}

/**
 * Log a message with trace level
 *
 * @param msg a message string
 */
fun logTrace(msg: String) {
    if (logType.value == LogType.ALL) {
        logMessage("TRACE", msg, OutputStreamType.STDOUT)
    }
}
