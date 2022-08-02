/**
 * Utilities to work with SAVE config in CLI mode
 */

package com.saveourtool.save.cli.config

import com.saveourtool.save.cli.ExitCodes
import com.saveourtool.save.cli.fs
import com.saveourtool.save.cli.logging.logErrorAndExit
import com.saveourtool.save.core.config.SaveProperties
import com.saveourtool.save.core.config.resolveSaveTomlConfig
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logType

import okio.FileNotFoundException
import okio.IOException
import okio.Path.Companion.toPath

/**
 * @param args CLI args
 * @return an instance of [SaveProperties]
 */
fun SaveProperties.Companion.of(args: Array<String>): SaveProperties {
    val configFromCli = try {
        parseArgs(fs, args)
    } catch (e: IOException) {
        return logErrorAndExit(
            ExitCodes.INVALID_CONFIGURATION,
            "Save expects to get the root directory for test files as the first CLI argument: save [cli-options] <test-root> [...]. " +
                    "Provided value to cli as a root for test directory and is not able to find it. " +
                    "Please provide a valid path to the root directory of test files. " +
                    "If you wanted to pass a configuration option instead, please check the list of available options using '--help'. " +
                    "Error details: ${e.message}"
        )
    }
    tryToUpdateDebugLevel(configFromCli)
    logDebug("Using the following properties for SAVE execution:\n${configFromCli.getFields()}")
    return configFromCli.validate()
}

/**
 * @return this config in case we have valid configuration
 */
private fun SaveProperties.validate(): SaveProperties {
    val fullConfigPath = testRootDir.toPath().resolveSaveTomlConfig()
    try {
        fs.metadata(fullConfigPath)
    } catch (e: FileNotFoundException) {
        return logErrorAndExit(
            ExitCodes.INVALID_CONFIGURATION, "Not able to find configuration file '$fullConfigPath'." +
                    " Please provide a valid path to the test config via command-line or using the file with properties."
        )
    }

    return this
}

/**
 * @return string which represents all fields of current instance
 */
private fun SaveProperties.getFields() = this.toString()
    .dropWhile { it != '(' }.drop(1)
    .dropLastWhile { it != ')' }.dropLast(1)

private fun tryToUpdateDebugLevel(properties: SaveProperties) {
    logType.set(properties.logType)
}
