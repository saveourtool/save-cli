// ******* !!! Automatically generated code, do not change *******
package org.cqfn.save.core.config

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.serialization.Serializable

/**
 * Configuration properties of save application, retrieved either from properties file
 * or from CLI args.
 * @property testConfig path to the configuration file
 * @property parallelMode whether to enable parallel mode
 * @property threads number of threads
 * @property debug turn on debug logging
 * @property quiet do not log anything
 * @property reportType type of generated report with execution results
 * @property baseline path to the file with baseline data
 * @property excludeSuites test suites, which won't be checked
 * @property includeSuites test suites, only which ones will be checked
 * @property language language that you are developing analyzer for
 * @property testRootPath path to directory with tests
 * @property resultOutput data output stream
 * @property configInheritance whether configuration files should inherit configurations from the previous level of directories
 * @property ignoreSaveComments if true, ignore technical comments, that SAVE uses to describe warnings, when running tests
 * @property reportDir path to directory where to store output (when `resultOutput` is set to `FILE`)
 * @property propertiesFile path to the file with extra properties (that can be also passed as command-line arguments)
 */
@Serializable
class SaveProperties(
    var testConfig: String? = "save.toml",
    var parallelMode: Boolean? = false,
    var threads: Int? = 1,
    var debug: Boolean? = false,
    var quiet: Boolean? = false,
    var reportType: ReportType? = ReportType.JSON,
    var baseline: String? = null,
    var excludeSuites: String? = null,
    var includeSuites: String? = null,
    var language: LanguageType? = LanguageType.UNDEFINED,
    var testRootPath: String? = null,
    var configInheritance: Boolean? = true,
    var ignoreSaveComments: Boolean? = false,
    var reportDir: String? = "save-reports",
    var resultOutput: ResultOutputType? = ResultOutputType.STDOUT,
    var propertiesFile: String? = null,
) {
    constructor(args: Array<String>) : this() {
        val parser = ArgParser("save")

        val testConfig by parser.option(
            ArgType.String,
            fullName = "test-config",
            shortName = "c",
            description = "Path to a configuration of a test suite",
        )

        val parallelMode by parser.option(
            ArgType.Boolean,
            fullName = "parallel-mode",
            shortName = "parallel",
            description = "Whether to enable parallel mode",
        )

        val threads by parser.option(
            ArgType.Int,
            fullName = "threads",
            shortName = "t",
            description = "Number of threads",
        )

        val debug by parser.option(
            ArgType.Boolean,
            fullName = "debug",
            shortName = "d",
            description = "Turn on debug logging"
        )

        val quiet by parser.option(
            ArgType.Boolean,
            fullName = "quiet",
            shortName = "q",
            description = "Do not log anything"
        )

        val reportType by parser.option(
            ArgType.Choice<ReportType>(),
            fullName = "report-type",
            description = "Possible types of output formats"
        )

        val baseline by parser.option(
            ArgType.String,
            fullName = "baseline",
            shortName = "b",
            description = "Path to the file with baseline data",
        )

        val excludeSuites by parser.option(
            ArgType.String,
            fullName = "exclude-suites",
            shortName = "e",
            description = "Test suites, which won't be checked",
        )

        val includeSuites by parser.option(
            ArgType.String,
            fullName = "includeSuites",
            shortName = "i",
            description = "Test suites, only which ones will be checked",
        )

        val language by parser.option(
            ArgType.Choice<LanguageType>(),
            fullName = "language",
            shortName = "l",
            description = "Language that you are developing analyzer for",
        )

        val testRootPath by parser.option(
            ArgType.String,
            fullName = "test-root-path",
            description = "Path to directory with tests (relative path from place, where save.properties is stored or absolute path)",
        )

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

        val propertiesFile by parser.option(
            ArgType.String,
            fullName = "properties-file",
            shortName = "prop",
            description = "Path to the file with configuration properties of save application aka save.properties",
        )

        parser.parse(args)

        this.propertiesFile = propertiesFile
        this.testConfig = testConfig
        this.parallelMode = parallelMode
        this.reportDir = reportDir
        this.ignoreSaveComments = ignoreSaveComments
        this.configInheritance = configInheritance
        this.resultOutput = resultOutput
        this.testRootPath = testRootPath
        this.language = language
        this.includeSuites = includeSuites
        this.excludeSuites = excludeSuites
        this.baseline = baseline
        this.propertiesFile = propertiesFile
        this.reportType = reportType
        this.quiet = quiet
        this.debug = debug
        this.threads = threads
    }

    /**
     * @param configFromPropertiesFile - config that will be used as a fallback in case when the field was not provided
     * @return this configuration
     */
    fun mergeConfigWithPriorityToThis(configFromPropertiesFile: SaveProperties): SaveProperties {
        testConfig = testConfig ?: configFromPropertiesFile.testConfig
        parallelMode = parallelMode ?: configFromPropertiesFile.parallelMode
        threads = threads ?: configFromPropertiesFile.threads
        debug = debug ?: configFromPropertiesFile.debug
        quiet = quiet ?: configFromPropertiesFile.quiet
        reportType = reportType ?: configFromPropertiesFile.reportType
        baseline = baseline ?: configFromPropertiesFile.baseline
        excludeSuites = excludeSuites ?: configFromPropertiesFile.excludeSuites
        includeSuites = includeSuites ?: configFromPropertiesFile.includeSuites
        language = language ?: configFromPropertiesFile.language
        testRootPath = testRootPath ?: configFromPropertiesFile.testRootPath
        configInheritance = configInheritance ?: configFromPropertiesFile.configInheritance
        ignoreSaveComments = ignoreSaveComments ?: configFromPropertiesFile.ignoreSaveComments
        reportDir = reportDir ?: configFromPropertiesFile.reportDir
        resultOutput = resultOutput ?: configFromPropertiesFile.resultOutput
        propertiesFile = propertiesFile ?: configFromPropertiesFile.propertiesFile
        return this
    }
}
