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

import kotlinx.cli.AbstractSingleOption
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.MultipleOption
import kotlinx.cli.default
import kotlinx.cli.multiple
import kotlinx.cli.required
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer
import org.cqfn.save.core.config.*
import org.cqfn.save.core.logging.logError

private fun <U> Map<String, String>.getAndParseOrElse(
    key: String,
    parse: String.() -> U,
    default: () -> U) =
        get(key)?.let(parse) ?: default()

/**
 * @param args CLI args
 * @return an instance of [SaveConfig]
 * @throws e
 */
@Suppress("TOO_LONG_FUNCTION")
fun createConfigFromArgs(args: Array<String>): SaveConfig {
    val parser = ArgParser("save")  // todo: or save-cli?

    val config by parser.option(
        ArgType.String,
        shortName = "c",
        description = "Path to the root save config file",
    )

    val parallelMode by parser.option(
        ArgType.Boolean,
        fullName = "parallel-mode",
        shortName = "parallel",
        description = "Whether to enable parallel mode",
    )

    val threads by parser.option(
        ArgType.Int,
        shortName = "t",
        description = "Number of threads",
    )

    val propertiesFile by parser.option(
        ArgType.String,
        fullName = "properties-file",
        shortName = "prop",
        description = "Path to the file with configuration properties of save application aka save.properties",
    )

    val debug by parser.option(
        ArgType.Boolean,
        shortName = "d",
        description = "Turn on debug logging"
    )

    val quiet by parser.option(
        ArgType.Boolean,
        shortName = "q",
        description = "Do not log anything"
    )

    val reportType by parser.option(
        ArgType.Choice<ReportType>(),
        fullName = "report-type",
        description = "Possible types of output formats"
    )

    val baseline: String? by (parser.option(
        ArgType.String,
        shortName = "b",
        description = "Path to the file with baseline data",
    ))

    val excludeSuites: List<String> by parser.option(
        ArgType.String,
        fullName = "exclude-suites",
        shortName = "e",
        description = "Test suites, which won't be checked",
    ).multiple()

    val includeSuites by parser.option(
        ArgType.String,
        fullName = "includeSuites",
        shortName = "i",
        description = "Test suites, only which ones will be checked",
    ).multiple()

    val language by parser.option(
        ArgType.Choice<LanguageType>(),
        shortName = "l",
        description = "Language that you are developing analyzer for",
    )

    val testRootPath by (parser.option(
        ArgType.String,
        fullName = "test-root-path",
        description = "Path to directory with tests (relative path from place, where save.properties is stored or absolute path)",
    ))

    val resultOutput by parser.option(
        ArgType.Choice<ResultOutputType>(),
        fullName = "result-output",
        shortName = "out",
        description = "Data output stream",
    )

    val configInheritance by parser.option(
        ArgType.Boolean,
        fullName = "config-inheritance",
        description = "Whether configuration files should inherit configurations from the previous level of directories",
    )

    val ignoreSaveComments by parser.option(
        ArgType.Boolean,
        fullName = "ignore-save-comments",
        description = "If true, ignore technical comments, that SAVE uses to describe warnings, when running tests",
    )

    val reportDir by parser.option(
        ArgType.String,
        fullName = "report-dir",
        description = "Path to directory, where to store output (when `resultOutput` is set to `FILE`)",
    )

    parser.parse(args)
    return SaveConfig(
        configPath = config.toPath(),
        parallelMode = parallelMode,
        threads = threads,
        propertiesFile = propertiesFile.toPath(),
        debug = debug,
        quiet = quiet,
        reportType = reportType,
        baselinePath = baseline?.toPath(),
        excludeSuites = excludeSuites,
        includeSuites = includeSuites,
        language = language,
        testRootPath = testRootPath.toPath(),
        resultOutput = resultOutput,
        configInheritance = configInheritance,
        ignoreSaveComments = ignoreSaveComments,
        reportDir = reportDir.toPath(),
    ).also {
        logDebug("Will be running SAVE with the following options: $it")
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun readPropertiesFile(propertiesFileName: String): SavePropertiesConfig {
    // The first iteration of parsing - get location of properties file. Ignore any other arguments.
/*    val propertiesFileOptionFullName = "properties-file"
    val propertiesFileOptionShortName = "prop"
    val propertiesFileName = args.toList().zipWithNext()
        .firstOrNull { it.first == "--$propertiesFileOptionFullName" || it.first == "-$propertiesFileOptionShortName" }
        ?.second
        ?: "save.properties"*/
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
    val deserializedProperties: SavePropertiesConfig = Properties.decodeFromMap(serializer(), properties)
    logInfo("Read properties from the properties file: $deserializedProperties")

    return deserializedProperties
}
