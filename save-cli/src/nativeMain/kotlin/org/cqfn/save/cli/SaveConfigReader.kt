/**
 * Utilities to work with SAVE config in CLI mode
 */

package org.cqfn.save.cli

import org.cqfn.save.cli.logging.logErrorAndExit
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logInfo

import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer
import okio.FileNotFoundException
import org.cqfn.save.core.Save
import org.cqfn.save.core.config.*

/**
 * @param args CLI args
 * @return an instance of [SaveCliConfig]
 */
@Suppress("TOO_LONG_FUNCTION")
fun createConfigFromArgs(args: Array<String>): SaveConfig {
    // getting configuration from command-line arguments
    val configFromCli = SaveConfig(args)
    // reading configuration from the properties file
    val configFromPropertiesFile = readPropertiesFile(configFromCli.propertiesFile)
    // merging two configurations into one [SaveConfig] with a priority to command line arguments
    return configFromCli.mergeConfigWithPriorityToThis(configFromPropertiesFile).validate()
}

/**
 * @param propertiesFileName path to the save.properties file
 * @return an instance of [SaveCliConfig] deserialized from this file
 */
@OptIn(ExperimentalSerializationApi::class)
fun readPropertiesFile(propertiesFileName: String?): SaveConfig {
    propertiesFileName?: return SaveConfig()

    logDebug("Reading properties file $propertiesFileName")

    val properties: Map<String, String> = try {
        FileSystem.SYSTEM.read(propertiesFileName.toPath()) {
            generateSequence { readUtf8Line() }.toList()
        }
            .associate { line ->
                line.split("=", limit = 2).let {
                    if (it.size != 2) {
                        logErrorAndExit(ExitCodes.GENERAL_ERROR,
                            "Incorrect format of property in $propertiesFileName" +
                                " Should be <key = value>, but was $line")
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

    logDebug("Read properties from the properties file: $properties")
    val deserializedPropertiesFile: SaveConfig = Properties.decodeFromMap(serializer(), properties)
    logInfo("Read properties from the properties file: $deserializedPropertiesFile")

    return deserializedPropertiesFile
}

fun SaveConfig.validate(): SaveConfig {
    try {
        FileSystem.SYSTEM.metadata(this.configPath?.toPath()!!)
    } catch(e: FileNotFoundException) {
        logErrorAndExit(ExitCodes.INVALID_CONFIGURATION, "Not able to find file '${this.configPath}'." +
                " Please provide a path to the valid test config via command-line or using the file with properties.")
    }

    return this
}
