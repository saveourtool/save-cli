package org.cqfn.save.core

import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.ResultOutputType
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.logging.isDebugEnabled
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.core.reporter.Reporter
import org.cqfn.save.core.reporter.afterAll
import org.cqfn.save.core.reporter.beforeAll
import org.cqfn.save.core.reporter.onEvent
import org.cqfn.save.core.reporter.onPluginExecutionEnd
import org.cqfn.save.core.reporter.onPluginExecutionError
import org.cqfn.save.core.reporter.onPluginExecutionStart
import org.cqfn.save.core.reporter.onPluginInitialization
import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
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
     */
    fun performAnalysis() {
        // get all toml configs in file system
        val testConfig = ConfigDetector().configFromFile(saveProperties.testConfig!!.toPath())
        requireNotNull(testConfig) { "Provided path ${saveProperties.testConfig} doesn't correspond to a valid save.toml file" }

        val out = when (saveProperties.resultOutput) {
            ResultOutputType.FILE -> fs.sink("save.out".toPath()).buffer()
            else -> TODO("Type ${saveProperties.resultOutput} is not yet supported")
        }
        // todo: make `saveProperties.reportType` a collection
        val reporter: Reporter = when (saveProperties.reportType) {
            ReportType.PLAIN -> PlainTextReporter(out)
            else -> TODO("Reporter for type ${saveProperties.reportType} is not yet supported")
        }
        reporter.beforeAll()

        val plugins: List<Plugin> = emptyList()  // todo: discover plugins (from configuration blocks in TestSuiteConfig?)
        logInfo("Discovered plugins: $plugins")
        plugins.forEach { plugin ->
            reporter.onPluginInitialization(plugin)
        }

        plugins.forEach { plugin ->
            logInfo("Execute plugin: ${plugin::class.simpleName}")
            reporter.onPluginExecutionStart(plugin)
            try {
                plugin.execute(testConfig)
                    .onEach { event -> reporter.onEvent(event) }
                    .forEach(this::handleResult)
            } catch (ex: PluginException) {
                reporter.onPluginExecutionError(ex)
                logError("${plugin::class.simpleName} has crashed: ${ex.message}")
            }
            logInfo("${plugin::class.simpleName} successfully executed!")
            reporter.onPluginExecutionEnd(plugin)
        }

        reporter.afterAll()
    }

    @Suppress("WHEN_WITHOUT_ELSE")  // TestResult is a sealed class
    private fun handleResult(testResult: TestResult) {
        val status = testResult.status
        when (status) {
            is Pass -> logDebug("Test on resources [${testResult.resources}] has completed successfully")
            is Fail -> logWarn("Test on resources [${testResult.resources}] has failed: ${status.reason}")
            is Ignored -> logWarn("Test on resources [${testResult.resources}] has been ignored: ${status.reason}")
            is Crash -> logError("Test on resources [${testResult.resources}] has crashed: ${status.throwable.message}." +
                    "Please report an issue at https://github.com/cqfn/save")
        }
        logDebug("Completed test execution for resources [${testResult.resources}]. Additional info: ${testResult.debugInfo}")
    }
}
