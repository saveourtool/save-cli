/**
 * Utilities to work with SAVE config in CLI mode
 */

package org.cqfn.save.cli

import org.cqfn.save.core.config.LanguageType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.ResultOutputType
import org.cqfn.save.core.config.SaveConfig

import okio.Path.Companion.toPath

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.multiple
import kotlinx.cli.required

/**
 * @param args CLI args
 * @return an instance of [SaveConfig]
 */
@Suppress("TOO_LONG_FUNCTION")
fun createConfigFromArgs(args: Array<String>): SaveConfig {
    val parser = ArgParser("save")

    val config by parser.option(
        ArgType.String,
        shortName = "c",
        description = "Path to the root save config file",
    ).default("save.toml")

    val parallelMode by parser.option(
        ArgType.Boolean,
        fullName = "parallel-mode",
        shortName = "parallel",
        description = "Whether to enable parallel mode",
    ).default(false)

    val threads by parser.option(
        ArgType.Int,
        shortName = "t",
        description = "Number of threads",
    ).default(1)

    val propertiesFile by parser.option(
        ArgType.String,
        fullName = "properties-file",
        shortName = "prop",
        description = "Path to the file with configuration properties of save application",
    ).default("save.properties")

    val debug by parser.option(
        ArgType.Boolean,
        shortName = "d",
        description = "Turn on debug logging"
    ).default(false)

    val quiet by parser.option(
        ArgType.Boolean,
        shortName = "q",
        description = "Do not log anything"
    ).default(false)

    val reportType by parser.option(
        ArgType.Choice<ReportType>(),
        fullName = "report-type",
        description = "Possible types of output formats"
    ).default(ReportType.JSON)

    val baseline by parser.option(
        ArgType.String,
        shortName = "b",
        description = "Path to the file with baseline data",
    )

    val excludeSuites by parser.option(
        ArgType.String,
        fullName = "exclude-suites",
        shortName = "e",
        description = "Test suites, which won't be checked",
    ).multiple()

    val includeSuites by parser.option(
        ArgType.String,
        fullName = "include-suites",
        shortName = "i",
        description = "Test suites, only which ones will be checked",
    ).multiple()

    val language by parser.option(
        ArgType.Choice<LanguageType>(),
        shortName = "l",
        description = "Language that you are developing analyzer for",
    ).default(LanguageType.JAVA)

    val testRootPath by parser.option(
        ArgType.String,
        fullName = "test-root-path",
        description = "Path to directory with tests (relative path from place, where save.properties is stored or absolute path)",
    ).required()

    val resultOutput by parser.option(
        ArgType.Choice<ResultOutputType>(),
        fullName = "result-output",
        shortName = "out",
        description = "Data output stream",
    ).default(ResultOutputType.STDOUT)

    val configInheritance by parser.option(
        ArgType.Boolean,
        fullName = "config-inheritance",
        description = "Whether configuration files should inherit configurations from the previous level of directories",
    ).default(true)

    val ignoreSaveComments by parser.option(
        ArgType.Boolean,
        fullName = "ignore-save-comments",
        description = "If true, ignore technical comments, that SAVE uses to describe warnings, when running tests",
    ).default(false)

    val reportDir by parser.option(
        ArgType.String,
        fullName = "report-dir",
        description = "Path to directory, where to store output (when `resultOutput` is set to `FILE`)",
    ).default("save-reports")

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
    )
}
