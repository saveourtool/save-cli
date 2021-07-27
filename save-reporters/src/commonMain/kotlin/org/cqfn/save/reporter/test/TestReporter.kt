package org.cqfn.save.reporter.test

import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.core.reporter.Reporter
import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult

import okio.BufferedSink

/**
 * Reporter that writes execution logs in plain text.
 *
 * @property out a sink for output
 */
class TestReporter(override val out: BufferedSink) : Reporter {
    val results: MutableList<TestResult> = mutableListOf()
    override val type: ReportType = ReportType.TEST

    override fun beforeAll() {
        logDebug("Initializing reporter ${this::class.qualifiedName} of type $type\n")
    }

    override fun afterAll() {
        logDebug("Finished reporter ${this::class.qualifiedName} of type $type\n")
    }

    override fun onSuiteStart(suiteName: String) {
    }

    override fun onSuiteEnd(suiteName: String) {
    }

    override fun onEvent(event: TestResult) {
        results.add(event)
    }

    override fun onPluginInitialization(plugin: Plugin) {
    }

    override fun onPluginExecutionStart(plugin: Plugin) {
    }

    override fun onPluginExecutionEnd(plugin: Plugin) {
    }

    override fun onPluginExecutionError(ex: PluginException) {
        out.write("Error during plugin execution: ${ex.describe()}\n".encodeToByteArray())
    }
}
