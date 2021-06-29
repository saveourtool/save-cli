package org.cqfn.save.core

import org.cqfn.save.core.config.OutputStreamType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.SAVE_VERSION
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.isSaveTomlConfig
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.files.StdStreamsSink
import org.cqfn.save.core.logging.isDebugEnabled
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.core.reporter.Reporter
import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.buildActivePlugins
import org.cqfn.save.core.utils.processInPlace
import org.cqfn.save.reporter.plain.PlainTextReporter

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

/**
 * @property saveProperties an instance of [SaveProperties]
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")  // todo: remove when there are >1 constructor parameters
class Save(
    private val saveProperties: SaveProperties
) {
    private val fs = FileSystem.SYSTEM

    init {
        isDebugEnabled = saveProperties.debug ?: false
    }

    /**
     * Main entrypoint for SAVE framework. Discovers plugins and calls their execution.
     *
     * @throws PluginException when we receive invalid type of PluginConfig
     */
    fun performAnalysis() {
        logInfo("Welcome to SAVE version $SAVE_VERSION")

        // FixMe: now we work only with the save.toml config and it's hierarchy, but we should work properly here with directories as well
        // constructing the file path to the configuration file
        val fullPathToConfig = with(saveProperties) {
            propertiesFile!!.toPath().parent!! / testRootPath!! / testConfigPath!!
        }
        val reporter = getReporter(saveProperties)
        val requestedConfigs = saveProperties.testFiles!!.filter {
            it.toPath().isSaveTomlConfig()
        }
        // get all toml configs in file system
        ConfigDetector()
            .configFromFile(fullPathToConfig)
            .getAllTestConfigsForFiles(requestedConfigs)
            .forEach { testConfig ->
                // iterating top-down
                reporter.beforeAll()

                testConfig
                    // fully process this config's configuration sections
                    .processInPlace()
                    // create plugins and choose only active (with test resources) ones
                    .buildActivePlugins(saveProperties.testFiles!!.filterNot {
                        it.toPath().isSaveTomlConfig()
                    }).forEach {
                        // execute created plugins
                        executePlugin(it, reporter)
                    }

                reporter.afterAll()
            }
        reporter.out.close()
    }

    private fun executePlugin(plugin: Plugin, reporter: Reporter) {
        reporter.onPluginInitialization(plugin)
        logDebug("=> Executing plugin: ${plugin::class.simpleName} for [${plugin.testConfig.location}]")
        reporter.onPluginExecutionStart(plugin)
        try {
            val events = plugin.execute().toList()
            if (events.isEmpty()) {
                logDebug("No resources discovered for ${plugin::class.simpleName} in [${plugin.testConfig.location}], skipping")
                reporter.onPluginExecutionSkip(plugin)
            }
            events
                .onEach { event -> reporter.onEvent(event) }
                .forEach(this::handleResult)
        } catch (ex: PluginException) {
            reporter.onPluginExecutionError(ex)
            logError("${plugin::class.simpleName} has crashed: ${ex.message}")
        }
        logDebug("<= Finished execution of: ${plugin::class.simpleName} for [${plugin.testConfig.location}]")
        reporter.onPluginExecutionEnd(plugin)
    }

    private fun getReporter(saveProperties: SaveProperties): Reporter {
        val out = when (val currentOutputType = saveProperties.resultOutput!!) {
            OutputStreamType.FILE -> fs.sink("save.out".toPath()).buffer()
            OutputStreamType.STDOUT, OutputStreamType.STDERR -> StdStreamsSink(currentOutputType).buffer()
        }
        // todo: make `saveProperties.reportType` a collection
        return when (saveProperties.reportType) {
            ReportType.PLAIN -> PlainTextReporter(out)
            else -> TODO("Reporter for type ${saveProperties.reportType} is not yet supported")
        }
    }

    @Suppress("WHEN_WITHOUT_ELSE")  // TestStatus is a sealed class
    private fun handleResult(testResult: TestResult) {
        when (val status = testResult.status) {
            is Pass -> {
                val passMessage = "Test on resources [${testResult.resources}] has completed successfully"
                status.message?.let { logDebug("$passMessage. $it") } ?: logDebug(passMessage)
            }
            is Fail -> logWarn("Test on resources [${testResult.resources}] has failed: ${status.reason}")
            is Ignored -> logWarn("Test on resources [${testResult.resources}] has been ignored: ${status.reason}")
            is Crash -> logError(
                "Test on resources [${testResult.resources}] has crashed: ${status.throwable.message}." +
                        "Please report an issue at https://github.com/cqfn/save"
            )
        }
        logDebug("Completed test execution for resources [${testResult.resources}]. Additional info: ${testResult.debugInfo}")
    }
}
