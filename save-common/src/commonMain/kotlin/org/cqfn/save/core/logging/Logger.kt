/**
 * Quick & dirty utilities for logging.
 * FixMe: Use proper logging solution once it's available for kotlin/native.
 */

package org.cqfn.save.core.logging

import org.cqfn.save.core.config.LogType
import org.cqfn.save.core.config.OutputStreamType

/**
 *  Logging mode
 */
expect var logType: LogType

/**
 * Whether to add time stamps to log messages
 */
expect var isTimeStampsEnabled: Boolean

/**
 * Log a message to the [stream] with timestamp and specific [level]
 *
 * @param level log level
 * @param msg a message string
 * @param stream output stream (file, stdout, stderr)
 */
expect fun logMessage(
    level: String,
    msg: String,
    stream: OutputStreamType
)

/**
 * Log a message with info level
 *
 * @param msg a message string
 */
expect fun logInfo(msg: String)

/**
 * Log a message with error level
 *
 * @param msg a message string
 */
expect fun logError(msg: String)

/**
 * Log a message with warn level
 *
 * @param msg a message string
 */
expect fun logWarn(msg: String)

/**
 * Log a message with debug level
 *
 * @param msg a message string
 */
expect fun logDebug(msg: String)

/**
 * Log a message with trace level
 *
 * @param msg a message string
 */
expect fun logTrace(msg: String)
