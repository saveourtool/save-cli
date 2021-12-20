package org.cqfn.save.core

import org.cqfn.save.core.config.OutputStreamType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.SAVE_VERSION
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.config.isSaveTomlConfig
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.files.StdStreamsSink
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.logging.logTrace
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.core.reporter.Reporter
import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.buildActivePlugins
import org.cqfn.save.core.utils.processInPlace
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPlugin
import org.cqfn.save.plugins.fix.FixPluginConfig
import org.cqfn.save.reporter.json.JsonReporter
import org.cqfn.save.reporter.plain.PlainOnlyFailedReporter
import org.cqfn.save.reporter.plain.PlainTextReporter
import org.cqfn.save.reporter.test.TestReporter

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

/**
 * @property saveProperties an instance of [SaveProperties]
 */
class Save(
    private val saveProperties: SaveProperties,
    private val fs: FileSystem,
) {
    /** reporter that can be used  */
    internal val reporter = getReporter(saveProperties)

    /**
     * Main entrypoint for SAVE framework. Discovers plugins and calls their execution.
     *
     * @return Reporter
     * @throws PluginException when we receive invalid type of PluginConfig
     */
    @Suppress("TOO_LONG_FUNCTION")
    fun performAnalysis(): Reporter {
        logInfo("Welcome to SAVE version $SAVE_VERSION")
        // FixMe: now we work only with the save.toml config and it's hierarchy, but we should work properly here with directories as well
        val testRootPath = saveProperties.testFiles!![0].toPath()
        val rootTestConfigPath = testRootPath / "save.toml"
        val (requestedConfigs, requestedTests) = saveProperties.testFiles!!
            .drop(1)
            .map { testRootPath / it }
            .map { it.toString() }
            .partition { it.toPath().isSaveTomlConfig() }
        val includeSuites = saveProperties.includeSuites?.split(",") ?: emptyList()
        val excludeSuites = saveProperties.excludeSuites?.split(",") ?: emptyList()

        reporter.beforeAll()

        // get all toml configs in file system
        val testConfigs = ConfigDetector(fs)
            .configFromFile(rootTestConfigPath)
            .getAllTestConfigsForFiles(requestedConfigs)
        var atLeastOneExecutionProvided = false
        testConfigs.forEach { testConfig ->
            // iterating top-down
            testConfig
                // fully process this config's configuration sections
                .processInPlace()
                .takeIf {
                    it.isFromEnabledSuite(includeSuites, excludeSuites)
                }
                // create plugins and choose only active (with test resources) ones
                ?.buildActivePlugins(requestedTests)
                ?.takeIf { plugins ->
                    plugins.isNotEmpty()
                }
                ?.also {
                    // configuration has been already validated by this point, and if there are active plugins, then suiteName is not null
                    reporter.onSuiteStart(testConfig.getGeneralConfig()?.suiteName!!)
                }
                ?.forEach {
                    atLeastOneExecutionProvided = true
                    // execute created plugins
                    executePlugin(it, reporter)
                }
                ?.also {
                    reporter.onSuiteEnd(testConfig.getGeneralConfig()?.suiteName!!)
                }
        }
        val saveToml = testConfigs.map { it.getGeneralConfig()?.configLocation }
        val excluded = testConfigs.map { it.getGeneralConfig()?.excludedTests?.toMutableList() ?: emptyList() }
            .reduceOrNull { acc, list -> acc + list }
        val excludedNote = excluded?.let {
            "Note: please check excludedTests: $excluded in following save.toml files: $saveToml"
        } ?: ""
        if (!atLeastOneExecutionProvided) {
            val warnMsg = if (requestedTests.isNotEmpty()) {
                """|Couldn't find any satisfied test resources for `$requestedTests`
                   |Please check the correctness of command and consider, that the last arguments treats as test file names for individual execution.
                   |$excludedNote
                """.trimMargin()
            } else {
                val fixPluginPatterns: String = getPluginPatterns<FixPluginConfig>(testConfigs)
                val warnPluginPatterns: String = getPluginPatterns<WarnPluginConfig>(testConfigs)
                "SAVE wasn't able to run tests, please check the correctness of configuration and test resources." +
                        "(fix plugin resourceNamePatternStrs: $fixPluginPatterns, warn plugin resourceNamePatternStrs: $warnPluginPatterns)"
            }
            logWarn(warnMsg)
        }
        reporter.afterAll()
        reporter.out.close()
        logInfo("SAVE has finished execution. You can rerun with --log debug or --log all for additional information.")

        return reporter
    }

    private inline fun <reified PluginConfigType : PluginConfig> getPluginPatterns(testConfigs: List<TestConfig>): String = testConfigs
        .last()
        .pluginConfigs
        .filterIsInstance<PluginConfigType>()
        .map { it.resourceNamePatternStr }
        .distinct()
        .joinToString(", ")

    private fun TestConfig.isFromEnabledSuite(includeSuites: List<String>, excludeSuites: List<String>): Boolean {
        val suiteName = getGeneralConfig()?.suiteName
        // either no specific includes, or current suite is included
        return (includeSuites.isEmpty() || includeSuites.contains(suiteName)) &&
                // either no specific excludes, or current suite is not excluded
                (excludeSuites.isEmpty() || !excludeSuites.contains(suiteName))
    }

    private fun executePlugin(plugin: Plugin, reporter: Reporter) {
        reporter.onPluginInitialization(plugin)
        logDebug("=> Executing plugin: ${plugin::class.simpleName} for [${plugin.testConfig.location}]")
        reporter.onPluginExecutionStart(plugin)
        try {
            val testRepositoryRootPath = plugin.testConfig.getRootConfig().location

            plugin.execute()
                .onEach { event ->
                    // calculate relative paths, because reporters don't need paths higher than root dir
                    val resourcesRelative = event.resources.withRelativePaths(testRepositoryRootPath)
                    reporter.onEvent(event.copy(resources = resourcesRelative))
                }
                .forEach(this::handleResult)
        } catch (ex: PluginException) {
            logError("${plugin::class.simpleName} has crashed: ${ex.message}")
            reporter.onPluginExecutionError(ex)
        }
        logDebug("<= Finished execution of: ${plugin::class.simpleName} for [${plugin.testConfig.location}]")
        reporter.onPluginExecutionEnd(plugin)
    }

    private fun getReporter(saveProperties: SaveProperties): Reporter {
        val outFileBaseName = "save.out"  // todo: make configurable
        val outFileName = when (saveProperties.reportType!!) {
            ReportType.PLAIN, ReportType.PLAIN_FAILED, ReportType.TEST -> outFileBaseName
            ReportType.JSON -> "$outFileBaseName.json"
            ReportType.XML -> "$outFileBaseName.xml"
            ReportType.TOML -> "$outFileBaseName.toml"
        }
        val out = when (val currentOutputType = saveProperties.resultOutput!!) {
            OutputStreamType.FILE -> fs.sink(outFileName.toPath()).buffer()
            OutputStreamType.STDOUT, OutputStreamType.STDERR -> StdStreamsSink(currentOutputType).buffer()
        }
        // todo: make `saveProperties.reportType` a collection
        return when (saveProperties.reportType) {
            ReportType.PLAIN -> PlainTextReporter(out)
            ReportType.PLAIN_FAILED -> PlainOnlyFailedReporter(out)
            ReportType.JSON -> JsonReporter(out) {
                FixPlugin.FixTestFiles.register(this)
            }
            ReportType.TEST -> TestReporter(out)
            else -> TODO("Reporter for type ${saveProperties.reportType} is not yet supported")
        }
    }

    @Suppress("WHEN_WITHOUT_ELSE")  // TestStatus is a sealed class
    private fun handleResult(testResult: TestResult) {
        when (val status = testResult.status) {
            is Pass -> {
                logDebug("\"Test on resources [${testResult.resources}] has completed successfully.\"")
                status.message?.let { logTrace(it) }
            }
            is Fail -> {
                logWarn("Test on resources [${testResult.resources}] has failed.")
                logTrace("Additional info: ${status.reason}.")
            }
            is Ignored -> {
                logWarn("Test on resources [${testResult.resources}] has been ignored.")
                logTrace("Additional info: ${status.reason}.")
            }
            is Crash -> logError(
                "Test on resources [${testResult.resources}] has crashed: ${status.description}." +
                        "Please report an issue at https://github.com/cqfn/save"
            )
        }
        logDebug("Completed test execution for resources [${testResult.resources}].")
        logTrace("Additional info: ${testResult.debugInfo}.")
    }
}
