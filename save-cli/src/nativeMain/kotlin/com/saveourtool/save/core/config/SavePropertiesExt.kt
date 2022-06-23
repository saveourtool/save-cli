/**
 * Utilities to work with SAVE config in CLI mode
 */

package com.saveourtool.save.core.config

import com.saveourtool.save.cli.ExitCodes
import com.saveourtool.save.cli.logging.logErrorAndExit
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logType

import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

private val fs: FileSystem = FileSystem.SYSTEM

/**
 * @param args CLI args
 * @return an instance of [SaveProperties]
 */
@Suppress("TOO_LONG_FUNCTION")
fun SaveProperties.Companion.of(args: Array<String>): SaveProperties {
    // getting configuration from command-line arguments
    if (args.isEmpty()) {
        return errorAndExitNotFoundDir()
    }
    val configFromCli = parseArgs(args) {
        // reading configuration from the properties file
        readPropertiesFile(it)
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
        FileSystem.SYSTEM.metadata(fullConfigPath)
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

/**
 * @param testRootDir path to a folder with the save.properties file
 * @return an instance of [SaveProperties] deserialized from this file
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TOO_LONG_FUNCTION")
private fun readPropertiesFile(testRootDir: String): SaveProperties {
    // reading configuration from the properties file
    val testRootDirPath = testRootDir.toPath()
    if (!fs.exists(testRootDirPath) || !fs.metadata(testRootDirPath).isDirectory) {
        return errorAndExitNotValidDir(testRootDirPath)
    }
    val propertiesFile = testRootDirPath / "save.properties"
    if (!fs.exists(propertiesFile)) {
        return SaveProperties(testRootDir = testRootDir)
    }
    logDebug("Reading properties from the file: $propertiesFile")

    val properties: Map<String, String> = try {
        fs.read(propertiesFile) {
            generateSequence { readUtf8Line() }.toList()
        }
            .associate { line ->
                line.split("=", limit = 2).let {
                    if (it.size != 2) {
                        return logErrorAndExit(ExitCodes.GENERAL_ERROR,
                            "Incorrect format of property in $propertiesFile" +
                                    " Should be <key = value>, but was <$line>")
                    }
                    it[0].trim() to it[1].trim()
                }
            }
    } catch (e: IOException) {
        return logErrorAndExit(
            ExitCodes.GENERAL_ERROR,
            "Failed to read properties file $propertiesFile: ${e.message}"
        )
    }
    require(!properties.contains("testRootDir")) {
        "The argument <testRootDir> can be set only via cli args"
    }
    logDebug("Found properties: $properties")
    return Properties.decodeFromStringMap(properties + ("testRootDir" to testRootDir))
}

private fun tryToUpdateDebugLevel(properties: SaveProperties) {
    logType.set(properties.logType)
}

private fun <R> errorAndExitNotFoundDir(): R = logErrorAndExit(ExitCodes.INVALID_CONFIGURATION,
    "Save expects to get the root directory for test files as the last CLI argument: save [cli-options] <test-root> [particular tests (optional)]" +
            "Save is not able to start processing without an information about the tests that should be run.")

private fun <R> errorAndExitNotValidDir(testRootPath: Path): R = logErrorAndExit(
    ExitCodes.INVALID_CONFIGURATION,
    "Save parsed the argument '$testRootPath' that you have provided to cli as a root for test directory and is not able to find it. " +
            "Please provide a valid path to the root directory of test files. " +
            "If you wanted to pass a configuration option instead, please check the list of available options using '--help'.")
