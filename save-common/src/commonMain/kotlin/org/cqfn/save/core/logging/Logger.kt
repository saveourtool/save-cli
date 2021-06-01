/**
 * Quick & dirty utilities for logging.
 * FixMe: Use proper logging solution once it's available for kotlin/native.
 */

package org.cqfn.save.core.logging

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


/**
 * Log a message with specific [level]
 *
 * @param level log level
 * @param msg a message string
 */
fun log(level: String, msg: String) {
    val currentTimeInstance = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val currentTime = "${currentTimeInstance.date} ${currentTimeInstance.hour}:${currentTimeInstance.minute}:${currentTimeInstance.second}"
    println("[$level] $currentTime: $msg")
}

/**
 * Is debug logging enabled
 */
var isDebugEnabled: Boolean = false

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
fun logWarn(msg: String): Unit = log("WARN", msg)

/**
 * Log a message with error level
 *
 * @param msg a message string
 */
fun logError(msg: String): Unit = log("ERROR", msg)
