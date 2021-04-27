package org.cqfn.save.core

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.Plugin
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

        val plugins: List<Plugin> = emptyList()  // todo: discover plugins (from configuration blocks in TestSuiteConfig?)
        logInfo("Discovered plugins: $plugins")
        plugins.forEach { plugin ->
            logInfo("Execute plugin: ${plugin::class.simpleName}")
            plugin.execute(saveProperties, testConfig).forEach(this::handleResult)
            logInfo("${plugin::class.simpleName} successfully executed!")
        }
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
