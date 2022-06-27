package com.saveourtool.save.core.utils

import com.saveourtool.save.core.logging.logTrace
import okio.FileSystem
import okio.IOException
import okio.Path

class PropertiesFileUtils {
    companion object {
        /**
         * @param fs implementation of [FileSystem]
         * @param propertiesFilePath path to a properties file
         * @return map of String to String with content of properties file
         */
        @Suppress("TOO_LONG_FUNCTION")
        fun read(fs: FileSystem, propertiesFilePath: Path): Map<String, String> {
            if (!fs.exists(propertiesFilePath)) {
                return emptyMap()
            }
            logTrace("Reading properties from the file: $propertiesFilePath")
            val properties: Map<String, String> = try {
                fs.read(propertiesFilePath) {
                    generateSequence { readUtf8Line() }.toList()
                }
                    .associate { line ->
                        line.split("=", limit = 2).let {
                            if (it.size != 2) {
                                throw IllegalArgumentException("Incorrect format of property in $propertiesFilePath" +
                                            " Should be <key = value>, but was <$line>")
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
    }
}