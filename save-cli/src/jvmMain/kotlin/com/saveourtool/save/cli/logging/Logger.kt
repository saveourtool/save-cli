/**
 * Logging utilities specific for native code.
 */

package com.saveourtool.save.cli.logging

import com.saveourtool.save.cli.ExitCodes
import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.logging.logWarn
import kotlin.system.exitProcess

actual fun logErrorAndExit(exitCode: ExitCodes, message: String): Nothing {
    logError(message)
    exitProcess(exitCode.code)
}

actual fun logWarn(messageSupplier: () -> String) {
    logWarn(messageSupplier())
}
