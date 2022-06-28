/**
 * Logging utilities specific for native code.
 */

package com.saveourtool.save.cli.logging

import com.saveourtool.save.cli.ExitCodes
import com.saveourtool.save.core.logging.logError
import kotlin.system.exitProcess

actual fun logErrorAndExit(exitCode: ExitCodes, message: String): Nothing {
    logError(message)
    exitProcess(exitCode.code)
}
