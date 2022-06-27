package com.saveourtool.save.core.utils

import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath

import kotlinx.cli.ArgParser
import kotlinx.cli.CLIEntity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

/**
 * Util class for processing CLI arguments
 */
class CliUtils {
    companion object {
        /**
         * Parse properties file ([rootDir]/[projectName].properties) to [T] structure
         *
         * @param fs implementation of [FileSystem]
         * @param rootDir
         * @param projectName
         * @return parsed [T] structure
         * @throws IOException when [rootDir] not found
         */
        @OptIn(ExperimentalSerializationApi::class)
        inline fun <reified T> parsePropertiesFile(
            fs: FileSystem,
            rootDir: String,
            projectName: String
        ): T {
            val rootDirPath = rootDir.toPath()
            if (!fs.exists(rootDirPath) || !fs.metadata(rootDirPath).isDirectory) {
                throw IOException("Invalid folder: $rootDir")
            }
            val propertiesFilePath = rootDirPath / "$projectName.properties"
            val propertiesFileContext = PropertiesFileUtils.read(fs, propertiesFilePath)
            return Properties.decodeFromStringMap(propertiesFileContext)
        }

        /**
         * Applies overriding from properties file
         *
         * @param cliEntity cli entry which is a delegate to parse value from CLI
         * @param valueFromCli
         * @param valueFromPropertiesFileOrDefault
         * @return if user set value by cli, returns this value. otherwise it returns value from properties file or default
         */
        fun <T, E : CLIEntity<T>> resolveValue(
            cliEntity: E,
            valueFromCli: T,
            valueFromPropertiesFileOrDefault: T
        ): T =
                if (cliEntity.valueOrigin == ArgParser.ValueOrigin.SET_BY_USER) {
                    valueFromCli
                } else {
                    valueFromPropertiesFileOrDefault
                }
    }
}
