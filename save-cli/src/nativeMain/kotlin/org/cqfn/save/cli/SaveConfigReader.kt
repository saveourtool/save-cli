/**
 * Utilities to work with SAVE config in CLI mode
 */

package org.cqfn.save.cli

import org.cqfn.save.cli.logging.logErrorAndExit
import org.cqfn.save.core.config.LogType
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.logging.GenericAtomicReference
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logTrace
import org.cqfn.save.core.logging.logType

import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.DIRECTORY_SEPARATOR
import okio.Path.Companion.toPath

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer

private val fs: FileSystem = FileSystem.SYSTEM

/**
 * @return this config in case we have valid configuration
 */
fun SaveProperties.validate(): SaveProperties {
    if (this.testFiles.isNullOrEmpty()) {
        logErrorAndExit(
            ExitCodes.INVALID_CONFIGURATION,
            "List with test files passed in CLI to save is missing or null. " +
                    "Save is not able to start processing without an information about the tests or test root path that should be used for execution."
        )
    }
    val testRootPath = testFiles!![0].toPath()
    try {
        if (!FileSystem.SYSTEM.metadata(testRootPath).isDirectory) {
            errorAndExitNotFoundDir()
        }
    } catch (e: FileNotFoundException) {
        errorAndExitNotValidDir(testRootPath)
    }
    val fullConfigPath = testRootPath / "save.toml"
    try {
        FileSystem.SYSTEM.metadata(fullConfigPath)
    } catch (e: FileNotFoundException) {
        logErrorAndExit(
            ExitCodes.INVALID_CONFIGURATION, "Not able to find configuration file '$fullConfigPath'." +
                    " Please provide a valid path to the test config via command-line or using the file with properties."
        )
    }

    return this
}

/**
 * @return string which represents all fields of current instance
 */
fun SaveProperties.getFields() = this.toString().dropWhile { it != '(' }.drop(1)
    .dropLast(1)

/**
 * @param args CLI args
 * @return an instance of [SaveProperties]
 */
@Suppress("TOO_LONG_FUNCTION")
fun createConfigFromArgs(args: Array<String>): SaveProperties {
    // getting configuration from command-line arguments
    if (args.isEmpty()) {
        errorAndExitNotFoundDir()
    }
    val configFromCli = SaveProperties(args)
    tryToUpdateDebugLevel(configFromCli)
    logTrace("Properties after parsed command line args:\n${configFromCli.getFields()}")
    // reading configuration from the properties file
    val testFiles = configFromCli.testFiles
    if (!testFiles.isNullOrEmpty() && !fs.exists(testFiles.first().toPath())) {
        errorAndExitNotValidDir(testFiles.first().toPath())
    }

    val testRootPath = if (testFiles.isNullOrEmpty() || !FileSystem.SYSTEM.metadata(testFiles.first().toPath()).isDirectory) {
        null
    } else {
        testFiles.first()
    }
    val propertiesFile = testRootPath + DIRECTORY_SEPARATOR + "save.properties"
    val configFromPropertiesFile = readPropertiesFile(propertiesFile)
    // merging two configurations into single [SaveProperties] class with a priority to command line arguments
    val mergedProperties = configFromCli.mergeConfigWithPriorityToThis(configFromPropertiesFile)
    tryToUpdateDebugLevel(mergedProperties)
    logDebug("Using the following properties for SAVE execution:\n${mergedProperties.getFields()}")
    return mergedProperties.validate()
}

/**
 * @param propertiesFileName path to the save.properties file
 * @return an instance of [SaveProperties] deserialized from this file
 */
@OptIn(ExperimentalSerializationApi::class)
fun readPropertiesFile(propertiesFileName: String?): SaveProperties {
    if (propertiesFileName == null || !FileSystem.SYSTEM.exists(propertiesFileName.toPath())) {
        return SaveProperties()
    }
    logDebug("Reading properties from the file: $propertiesFileName")

    val properties: Map<String, String> = try {
        FileSystem.SYSTEM.read(propertiesFileName.toPath()) {
            generateSequence { readUtf8Line() }.toList()
        }
            .associate { line ->
                line.split("=", limit = 2).let {
                    if (it.size != 2) {
                        logErrorAndExit(ExitCodes.GENERAL_ERROR,
                            "Incorrect format of property in $propertiesFileName" +
                                    " Should be <key = value>, but was <$line>")
                    }
                    it[0].trim() to it[1].trim()
                }
            }
    } catch (e: IOException) {
        logErrorAndExit(
            ExitCodes.GENERAL_ERROR,
            "Failed to read properties file $propertiesFileName: ${e.message}"
        )
    }

    logDebug("Found properties: $properties")
    return Properties.decodeFromStringMap(serializer(), properties)
}

private fun tryToUpdateDebugLevel(properties: SaveProperties) {
    logType = GenericAtomicReference(properties.logType ?: LogType.WARN)
}

private fun errorAndExitNotFoundDir() {
    logErrorAndExit(ExitCodes.INVALID_CONFIGURATION,
        "Save expects to get the root directory for test files as the last CLI argument: save [cli-options] <test-root> [particular tests (optional)]" +
                "Save is not able to start processing without an information about the tests that should be run.")
}

private fun errorAndExitNotValidDir(testRootPath: Path) {
    logErrorAndExit(
        ExitCodes.INVALID_CONFIGURATION,
        "Save parsed the argument '$testRootPath' that you have provided to cli as a root for test directory and is not able to find it. " +
                "Please provide a valid path to the root directory of test files. " +
                "If you wanted to pass a configuration option instead, please check the list of available options using '--help'.")
}
