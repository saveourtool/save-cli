/**
 * Logging utilities specific for native code.
 */

package com.saveourtool.save.cli.logging

import com.saveourtool.save.cli.ExitCodes

/**
 * Log result of [messageSupplier] with level WARN
 *
 * @param messageSupplier supplier for message to log
 */
expect fun logWarn(messageSupplier: () -> String): Unit

/**
 * Log [message] with level ERROR and exit process with code [exitCode]
 *
 * @param exitCode exit code
 * @param message message to log
 * @return nothing, program terminates in this method
 */
internal expect fun logErrorAndExit(exitCode: ExitCodes, message: String): Nothing
