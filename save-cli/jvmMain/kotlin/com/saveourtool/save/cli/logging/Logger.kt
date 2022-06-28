/**
 * Logging utilities specific for native code.
 */

package com.saveourtool.save.cli.logging

import com.saveourtool.save.cli.ExitCodes
import com.saveourtool.save.core.logging.logError
import kotlin.system.exitProcess

/**
 * Log [message] with level ERROR and exit process with code [exitCode]
 *
 * @param exitCode exit code
 * @param message message to log
 * @return nothing, program terminates in this method
 */
@Deprecated("never use this method in save-core as it can lead to a break of save-cloud application")
fun logErrorAndExit(exitCode: ExitCodes, message: String): Nothing {
    logError(message)
    exitProcess(exitCode.code)
}
