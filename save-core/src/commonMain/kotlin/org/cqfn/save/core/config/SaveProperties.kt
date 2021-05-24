//  ---------------------------------------------------------------------
//  ******* This file was auto generated, please don't modify it. *******
//  ---------------------------------------------------------------------
package org.cqfn.save.core.config

import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.serialization.Serializable

/**
 * Configuration properties of save application, retrieved either from properties file
 * or from CLI args.
 * @property testConfigName Path to a configuration of a test suite
 * @property parallelMode Whether to enable parallel mode
 * @property threads Number of threads
 * @property propertiesFile Path to the file with configuration properties of save application aka
 * save.properties
 * @property debug Turn on debug logging
 * @property quiet Do not log anything
 * @property reportType Type of generated report with execution results
 * @property baseline Path to the file with baseline data
 * @property excludeSuites Test suites, which won't be checked
 * @property includeSuites Test suites, only which ones will be checked
 * @property language Language that you are developing analyzer for
 * @property testRootPath Path to directory with tests (relative path from place, where
 * save.properties is stored or absolute path)
 * @property resultOutput Data output stream
 * @property configInheritance Whether configuration files should inherit configurations from the
 * previous level of directories
 * @property ignoreSaveComments If true, ignore technical comments, that SAVE uses to describe
 * warnings, when running tests
 * @property reportDir Path to directory, where to store output (when `resultOutput` is set to
 * `FILE`)
 */
@Serializable
public data class SaveProperties(
    public var testConfigName: String? = "save.toml",
    public var parallelMode: Boolean? = false,
    public var threads: Int? = 1,
    public var propertiesFile: String? = "save.properties",
    public var debug: Boolean? = false,
    public var quiet: Boolean? = false,
    public var reportType: ReportType? = ReportType.JSON,
    public var baseline: String? = null,
    public var excludeSuites: String? = null,
    public var includeSuites: String? = null,
    public var language: LanguageType? = LanguageType.UNDEFINED,
    public var testRootPath: String? = null,
    public var resultOutput: ResultOutputType? = ResultOutputType.STDOUT,
    public var configInheritance: Boolean? = true,
    public var ignoreSaveComments: Boolean? = false,
    public var reportDir: String? = "save-reports"
) {
    public constructor(args: Array<String>) : this() {
        val parser = ArgParser("save")
        val testConfigName by parser.option(
                ArgType.String,
                fullName = "test-config",
                shortName = "c",
                description = "Path to a configuration of a test suite"
                )

        val parallelMode by parser.option(
                ArgType.Boolean,
                fullName = "parallel-mode",
                shortName = "parallel",
                description = "Whether to enable parallel mode"
                )

        val threads by parser.option(
                ArgType.Int,
                fullName = "threads",
                shortName = "t",
                description = "Number of threads"
                )

        val propertiesFile by parser.option(
                ArgType.String,
                fullName = "properties-file",
                shortName = "prop",
                description =
                        "Path to the file with configuration properties of save application aka save.properties"
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
                description = "Type of generated report with execution results"
                )

        val baseline by parser.option(
                ArgType.String,
                fullName = "baseline",
                shortName = "b",
                description = "Path to the file with baseline data"
                )

        val excludeSuites by parser.option(
                ArgType.String,
                fullName = "exclude-suites",
                shortName = "e",
                description = "Test suites, which won't be checked"
                )

        val includeSuites by parser.option(
                ArgType.String,
                fullName = "include-suites",
                shortName = "i",
                description = "Test suites, only which ones will be checked"
                )

        val language by parser.option(
                ArgType.Choice<LanguageType>(),
                fullName = "language",
                shortName = "l",
                description = "Language that you are developing analyzer for"
                )

        val testRootPath by parser.option(
                ArgType.String,
                fullName = "test-root-path",
                description =
                        "Path to directory with tests (relative path from place, where save.properties is stored or absolute path)"
                )

        val resultOutput by parser.option(
                ArgType.Choice<ResultOutputType>(),
                fullName = "result-output",
                shortName = "out",
                description = "Data output stream"
                )

        val configInheritance by parser.option(
                ArgType.Boolean,
                fullName = "config-inheritance",
                description =
                        "Whether configuration files should inherit configurations from the previous level of directories"
                )

        val ignoreSaveComments by parser.option(
                ArgType.Boolean,
                fullName = "ignore-save-comments",
                description =
                        "If true, ignore technical comments, that SAVE uses to describe warnings, when running tests"
                )

        val reportDir by parser.option(
                ArgType.String,
                fullName = "report-dir",
                description =
                        "Path to directory, where to store output (when `resultOutput` is set to `FILE`)"
                )

        parser.parse(args)
        this.testConfigName = testConfigName
        this.parallelMode = parallelMode
        this.threads = threads
        this.propertiesFile = propertiesFile
        this.debug = debug
        this.quiet = quiet
        this.reportType = reportType
        this.baseline = baseline
        this.excludeSuites = excludeSuites
        this.includeSuites = includeSuites
        this.language = language
        this.testRootPath = testRootPath
        this.resultOutput = resultOutput
        this.configInheritance = configInheritance
        this.ignoreSaveComments = ignoreSaveComments
        this.reportDir = reportDir
    }

    /**
     * @param configFromPropertiesFile - config that will be used as a fallback in case when the
     * field was not provided
     * @return this configuration
     */
    public fun mergeConfigWithPriorityToThis(configFromPropertiesFile: SaveProperties):
            SaveProperties {
        testConfigName = testConfigName ?: configFromPropertiesFile.testConfigName
                parallelMode = parallelMode ?: configFromPropertiesFile.parallelMode
                threads = threads ?: configFromPropertiesFile.threads
                propertiesFile = propertiesFile ?: configFromPropertiesFile.propertiesFile
                debug = debug ?: configFromPropertiesFile.debug
                quiet = quiet ?: configFromPropertiesFile.quiet
                reportType = reportType ?: configFromPropertiesFile.reportType
                baseline = baseline ?: configFromPropertiesFile.baseline
                excludeSuites = excludeSuites ?: configFromPropertiesFile.excludeSuites
                includeSuites = includeSuites ?: configFromPropertiesFile.includeSuites
                language = language ?: configFromPropertiesFile.language
                testRootPath = testRootPath ?: configFromPropertiesFile.testRootPath
                resultOutput = resultOutput ?: configFromPropertiesFile.resultOutput
                configInheritance = configInheritance ?: configFromPropertiesFile.configInheritance
                ignoreSaveComments = ignoreSaveComments ?:
                        configFromPropertiesFile.ignoreSaveComments
                reportDir = reportDir ?: configFromPropertiesFile.reportDir
        return this
    }
}
