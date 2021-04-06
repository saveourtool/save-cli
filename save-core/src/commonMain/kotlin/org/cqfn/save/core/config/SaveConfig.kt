package org.cqfn.save.core.config

import okio.ExperimentalFileSystem
import okio.Path

/**
 * Configuration properties of save application, retrieved either from properties file
 * or from CLI args.
 * @property configPath path to the configuration file
 * @property parallelMode whether to enable parallel mode
 * @property threads number of threads
 * @property propertiesFile path to the file with configuration properties of save application
 * @property debug turn on debug logging
 * @property quiet do not log anything
 * @property reportType type of generated report with execution results
 * @property baselinePath path to the file with baseline data
 * @property excludeSuites test suites, which won't be checked
 * @property includeSuites test suites, only which ones will be checked
 * @property language language that you are developing analyzer for
 * @property testRootPath path to directory with tests
 * @property resultOutput data output stream
 * @property configInheritance whether configuration files should inherit configurations from the previous level of directories
 * @property ignoreTechnicalComments if true, ignore technical comments, that SAVE uses to describe warnings, when running tests
 * @property reportDir path to directory where to store output (when `resultOutput` is set to `FILE`)
 * @property runSingleTest path to the file with 'Test' postfix, which need to be run in single mode
 */
@OptIn(ExperimentalFileSystem::class)
data class SaveConfig(
    val configPath: Path,
    val parallelMode: Boolean,
    val threads: Int,
    val propertiesFile: Path,
    val debug: Boolean,
    val quiet: Boolean,
    val reportType: ReportType,
    val baselinePath: Path?,
    val excludeSuites: List<String>,
    val includeSuites: List<String>,
    val language: LanguageType,
    val testRootPath: Path,
    val resultOutput: ResultOutputType,
    val configInheritance: Boolean,
    val ignoreTechnicalComments: Boolean,
    val reportDir: Path,
    val runSingleTest: Path?
)
