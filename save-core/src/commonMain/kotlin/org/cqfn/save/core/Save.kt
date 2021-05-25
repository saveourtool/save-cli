package org.cqfn.save.core

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.logging.isDebugEnabled
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

/**
 * @property saveProperties an instance of [SaveProperties]
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")  // todo: remove when there are >1 constructor parameters
class Save(
    private val saveProperties: SaveProperties
) {
    init {
        isDebugEnabled = saveProperties.debug ?: false
    }

    /**
     * Main entrypoint for SAVE framework. Discovers plugins and calls their execution.
     */
    fun performAnalysis() {
        // get all toml configs in file system
        val testConfig = ConfigDetector().configFromFile(saveProperties.testConfig)
        val plugins: List<Plugin> = emptyList()  // todo: discover plugins (from configuration blocks in TestSuiteConfig?)
        logInfo("Discovered plugins: $plugins")
        plugins.forEach { plugin ->
            logInfo("Execute plugin: ${plugin::class.simpleName}")
            plugin.execute(testConfig).forEach(this::handleResult)
            logInfo("${plugin::class.simpleName} successfully executed!")
        }
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
