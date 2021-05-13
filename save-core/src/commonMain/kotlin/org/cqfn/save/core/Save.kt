package org.cqfn.save.core

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.files.ConfigDetector
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

import okio.Path.Companion.toPath

/**
 * @property saveProperties an instance of [SaveProperties]
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")  // todo: remove when there are >1 constructor parameters
class Save(
    private val saveProperties: SaveProperties
) {
    /**
     * Main entrypoint for SAVE framework. Discovers plugins and calls their execution.
     */
    fun performAnalysis() {
        // get all toml configs in file system
        val testConfig = ConfigDetector().configFromFile(saveProperties.testConfig!!.toPath())
        requireNotNull(testConfig) { "Provided path ${saveProperties.testConfig} doesn't correspond to a valid save.toml file" }

        val reporters: List<Reporter> = emptyList()  // todo: create reporters based on saveProperties.reportType (also it should be a collection?)
        // todo: provide correct output sink for reporter based on properties.reportOutputType
        reporters.beforeAll()

        val plugins: List<Plugin> = emptyList()  // todo: discover plugins (from configuration blocks in TestSuiteConfig?)
        logInfo("Discovered plugins: $plugins")
        plugins.forEach { plugin ->
            reporters.onPluginInitialization(plugin)
        }

        plugins.forEach { plugin ->
            logInfo("Execute plugin: ${plugin::class.simpleName}")
            reporters.onPluginExecutionStart(plugin)
            try {
                plugin.execute(saveProperties, testConfig)
                    .onEach { event -> reporters.onEvent(event) }
                    .forEach(this::handleResult)
            } catch (ex: PluginException) {
                reporters.onPluginExecutionError(ex)
                logError("${plugin::class.simpleName} has crashed: ${ex.message}")
            }
            logInfo("${plugin::class.simpleName} successfully executed!")
            reporters.onPluginExecutionEnd(plugin)
        }

        reporters.afterAll()
    }

    @Suppress("WHEN_WITHOUT_ELSE")  // TestResult is a sealed class
    private fun handleResult(testResult: TestResult) {
        when (testResult.status) {
            is Pass -> logDebug("Test on resources [${testResult.resources}] has completed successfully")
            is Fail -> logWarn("Test on resources [${testResult.resources}] has failed: ${testResult.status.reason}")
            is Ignored -> logWarn("Test on resources [${testResult.resources}] has been ignored: ${testResult.status.reason}")
            is Crash -> logError("Test on resources [${testResult.resources}] has crashed: ${testResult.status.throwable.message}." +
                    "Please report an issue at https://github.com/cqfn/save")
        }
        logDebug("Completed test execution for resources [${testResult.resources}]. Additional info: ${testResult.debugInfo}")
    }
}
