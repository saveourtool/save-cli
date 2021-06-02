/**
 * Quick & dirty utilities for logging.
 * FixMe: Use proper logging solution once it's available for kotlin/native.
 */

package org.cqfn.save.core.logging

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.cqfn.save.core.config.ResultOutputType

/**
 * Is debug logging enabled
 */
var isDebugEnabled: Boolean = true

expect fun logToStream(level: String, msg: String, output: ResultOutputType)

/**
 * Log a message with specific [level]
 *
 * @param level log level
 * @param msg a message string
 * @param output output stream (file, stdout, stderr)
 */
fun log(level: String, msg: String, output: ResultOutputType = ResultOutputType.STDOUT) {
    val currentTimeInstance = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val currentTime = "${currentTimeInstance.date} ${currentTimeInstance.hour}:${currentTimeInstance.minute}:${currentTimeInstance.second}"
    logToStream(level, "[$level] $currentTime: $msg", output)
}

/**
 * Log a message with debug level
 *
 * @param msg a message string
 */
fun logDebug(msg: String) {
    if (isDebugEnabled) {
        log("DEBUG", msg)
    }
}

/**
 * Log a message with info level
 *
 * @param msg a message string
 */
fun logInfo(msg: String): Unit = log("INFO", msg)

/**
 * Log a message with warn level
 *
 * @param msg a message string
 */
fun logWarn(msg: String): Unit = log("WARN", msg, ResultOutputType.STDERR)

/**
 * Log a message with error level
 *
 * @param msg a message string
 */
fun logError(msg: String): Unit = log("ERROR", msg, ResultOutputType.STDERR)
