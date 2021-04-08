/**
 * Quick & dirty utilities for logging.
 * FixMe: Use proper logging solution once it's available for kotlin/native.
 */

package org.cqfn.save.core.logging

/**
 * todo: configure via --debug option
 */
val isDebugEnabled = false

/**
 * Log a message with debug level
 *
 * @param msg a message string
 */
fun logDebug(msg: String) {
    if (isDebugEnabled) {
        println("DEBUG: $msg")
    }
}

/**
 * Log a message with info level
 *
 * @param msg a message string
 */
fun logInfo(msg: String): Unit = println("INFO: $msg")

/**
 * Log a message with warn level
 *
 * @param msg a message string
 */
fun logWarn(msg: String): Unit = println("WARN: $msg")

/**
 * Log a message with error level
 *
 * @param msg a message string
 */
fun logError(msg: String): Unit = println("ERROR: $msg")
