/**
 * Quick & dirty utilities for logging.
 * FixMe: Use proper logging solution once it's available for kotlin/native.
 */

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
actual var logType: LogType = LogType.WARN

/**
 * Whether to add time stamps to log messages
 */
actual var isTimeStampsEnabled: Boolean = false

/**
 * Log a message to the [stream] with timestamp and specific [level]
 *
 * @param level log level
 * @param msg a message string
 * @param stream output stream (file, stdout, stderr)
 */
actual fun logMessage(
    level: String,
    msg: String,
    stream: OutputStreamType
) {
    error("Not implemented yet")
}

/**
 * Log a message with info level
 *
 * @param msg a message string
 */
actual fun logInfo(msg: String) {
    error("Not implemented yet")
}

/**
 * Log a message with error level
 *
 * @param msg a message string
 */
actual fun logError(msg: String) {
    error("Not implemented yet")
}

/**
 * Log a message with warn level
 *
 * @param msg a message string
 */
actual fun logWarn(msg: String) {
    error("Not implemented yet")
}

/**
 * Log a message with debug level
 *
 * @param msg a message string
 */
actual fun logDebug(msg: String) {
    error("Not implemented yet")
}

/**
 * Log a message with trace level
 *
 * @param msg a message string
 */
actual fun logTrace(msg: String) {
    error("Not implemented yet")
}
