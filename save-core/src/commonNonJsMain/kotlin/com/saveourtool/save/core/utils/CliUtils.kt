/**
 * Util class for processing CLI arguments
 */

package com.saveourtool.save.core.utils

import com.saveourtool.save.core.logging.logTrace
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

import kotlinx.cli.ArgParser
import kotlinx.cli.CLIEntity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

/**
 * Parse properties file ([rootDir]/[projectName].properties) to [T] structure
 *
 * @param rootDir
 * @param projectName
 * @return parsed [T] structure
 * @throws IOException when [rootDir] not found
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> FileSystem.parsePropertiesFile(
    rootDir: String,
    projectName: String
): T {
    val rootDirPath = rootDir.toPath()
    if (!this.exists(rootDirPath) || !this.metadata(rootDirPath).isDirectory) {
        throw IOException("Invalid folder: $rootDir")
    }
    val propertiesFilePath = rootDirPath / "$projectName.properties"
    val propertiesFileContext = this.readPropertiesFile(propertiesFilePath)
    return Properties.decodeFromStringMap(propertiesFileContext)
}

/**
 * @param propertiesFilePath path to a properties file
 * @return map of String to String with content of properties file
 * @throws IOException failed to read properties file
 * @throws IllegalArgumentException properties files has invalid format
 */
@Suppress("TOO_LONG_FUNCTION")
fun FileSystem.readPropertiesFile(propertiesFilePath: Path): Map<String, String> {
    if (!this.exists(propertiesFilePath)) {
        return emptyMap()
    }
    logTrace("Reading properties from the file: $propertiesFilePath")
    val properties: Map<String, String> = try {
        this.read(propertiesFilePath) {
            generateSequence { readUtf8Line() }.toList()
        }
            .associate { line ->
                line.split("=", limit = 2).let {
                    if (it.size != 2) {
                        throw IllegalArgumentException(
                            "Incorrect format of property in $propertiesFilePath" +
                                    " Should be <key = value>, but was <$line>"
                        )
                    }
                    it[0].trim() to it[1].trim()
                }
            }
    } catch (e: IOException) {
        throw IOException("Failed to read properties file $propertiesFilePath: ${e.message}", e)
    }
    logTrace("Read properties: $properties")
    return properties
}

/**
 * Applies overriding from properties file based on [CLIEntity]
 *
 * @param valueFromCli
 * @param valueFromPropertiesFileOrDefault
 * @return if user set value by cli, returns this value. otherwise it returns value from properties file or default
 */
fun <T> CLIEntity<T>.resolveValue(valueFromCli: T, valueFromPropertiesFileOrDefault: T): T =
        if (this.valueOrigin == ArgParser.ValueOrigin.SET_BY_USER) {
            valueFromCli
        } else {
            valueFromPropertiesFileOrDefault
        }
