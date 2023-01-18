/**
 * Logging utilities specific for native code.
 */

package com.saveourtool.save.cli.logging

import com.saveourtool.save.cli.ExitCodes

/**
 * Log [message] with level ERROR and exit process with code [exitCode]
 *
 * @param exitCode exit code
 * @param message message to log
 * @return nothing, program terminates in this method
 */
@Deprecated("never use this method in save-core as it can lead to a break of save-cloud application")
expect fun logErrorAndExit(exitCode: ExitCodes, message: String): Nothing

/**
 * Log result of [messageSupplier] with level WARN
 *
 * @param messageSupplier supplier for message to log
 */
expect fun logWarn(messageSupplier: () -> String): Unit
