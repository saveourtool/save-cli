package com.saveourtool.save.core.utils

import kotlinx.cli.ArgParser
import kotlinx.cli.CLIEntity
import kotlinx.serialization.ExperimentalSerializationApi
import okio.Path.Companion.toPath

import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import okio.FileSystem
import okio.IOException

class CliUtils {
    companion object {

        /**
         * Parse properties file ([rootDir]/[projectName].properties) to [T] structure
         *
         * @param rootDir
         * @param projectName
         * @return parsed [T] structure
         * @throws IOException when [rootDir] not found
         */
        @OptIn(ExperimentalSerializationApi::class)
        inline fun <reified T> parsePropertiesFile(rootDir: String, projectName: String): T {
            val rootDirPath = rootDir.toPath()
            if (!FileSystem.SYSTEM.exists(rootDirPath) || !FileSystem.SYSTEM.metadata(rootDirPath).isDirectory) {
                throw IOException("Invalid folder: $rootDir")
            }
            val propertiesFilePath = rootDirPath / "$projectName.properties"
            val propertiesFileContext = PropertiesFileUtils.read(propertiesFilePath)
            return Properties.decodeFromStringMap(propertiesFileContext)
        }

        /**
         * Applies overriding from properties file
         * @return if user set value by cli, returns this value. otherwise it returns value from properties file or default
         */
        fun <T, E : CLIEntity<T>> resolveValue(cliEntity: E, valueFromCli: T, valueFromPropertiesFileOrDefault: T): T =
            if (cliEntity.valueOrigin == ArgParser.ValueOrigin.SET_BY_USER) {
                valueFromCli
            } else {
                valueFromPropertiesFileOrDefault
            }
    }
}