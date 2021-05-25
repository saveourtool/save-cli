/**
 * Utilities to work with SAVE config in CLI mode
 */

package org.cqfn.save.cli

import org.cqfn.save.cli.logging.logErrorAndExit
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logInfo

import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.DIRECTORY_SEPARATOR
import okio.Path.Companion.toPath

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer

/**
 * @return this config in case we have valid configuration
 */
fun SaveProperties.validate(): SaveProperties {
    val fullConfigPath = testRootPath + DIRECTORY_SEPARATOR + testConfigPath
    try {
        this.testRootPath ?: logErrorAndExit(ExitCodes.INVALID_CONFIGURATION,
            "`testRootPath` option is missing or null. " +
                    "Save is not able to start processing without an information about the tests that should be run.")

        FileSystem.SYSTEM.metadata(fullConfigPath.toPath())
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
    val configFromCli = SaveProperties(args)
    logDebug("Properties after parsed command line args:\n${configFromCli.getFields()}")
    // reading configuration from the properties file
    val configFromPropertiesFile = readPropertiesFile(configFromCli.propertiesFile)
    // merging two configurations into single [SaveProperties] class with a priority to command line arguments
    val mergedProperties = configFromCli.mergeConfigWithPriorityToThis(configFromPropertiesFile)
    logInfo("Using the following properties for SAVE execution:\n${mergedProperties.getFields()}")
    return mergedProperties.validate()
}

/**
 * @param propertiesFileName path to the save.properties file
 * @return an instance of [SaveProperties] deserialized from this file
 */
@OptIn(ExperimentalSerializationApi::class)
fun readPropertiesFile(propertiesFileName: String?): SaveProperties {
    propertiesFileName ?: return SaveProperties()

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
