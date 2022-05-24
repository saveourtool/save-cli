package com.saveourtool.save.reporter.test

import com.saveourtool.save.core.config.ReportType
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.plugin.PluginException
import com.saveourtool.save.core.reporter.Reporter
import com.saveourtool.save.core.result.TestResult

import okio.BufferedSink

/**
 * Reporter that writes execution logs in plain text.
 *
 * @property out a sink for output
 *
 */
class TestReporter(override val out: BufferedSink) : Reporter {
    /** list with results */
    val results: MutableList<TestResult> = mutableListOf()
    override val type: ReportType = ReportType.TEST

    override fun beforeAll() {
        logDebug("Initializing reporter ${this::class.qualifiedName} of type $type\n")
    }

    override fun afterAll() {
        logDebug("Finished reporter ${this::class.qualifiedName} of type $type\n")
    }

    override fun onSuiteStart(suiteName: String) {
        logDebug("Starting $suiteName")
    }

    override fun onSuiteEnd(suiteName: String) {
        logDebug("Ending $suiteName")
    }

    override fun onEvent(event: TestResult) {
        results.add(event)
    }

    override fun onPluginInitialization(plugin: Plugin) {
        logDebug("Init $plugin")
    }

    override fun onPluginExecutionStart(plugin: Plugin) {
        logDebug("Exec start $plugin")
    }

    override fun onPluginExecutionEnd(plugin: Plugin) {
        logDebug("Exec end $plugin")
    }

    override fun onPluginExecutionError(ex: PluginException) {
        out.write("Error during plugin execution: ${ex.describe()}\n".encodeToByteArray())
    }
}
