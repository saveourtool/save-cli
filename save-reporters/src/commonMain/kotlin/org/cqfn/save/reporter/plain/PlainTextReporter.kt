package org.cqfn.save.reporter.plain

import org.cqfn.save.core.config.ReportType
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
class PlainTextReporter(override val out: BufferedSink) : Reporter {
    override val type: ReportType = ReportType.PLAIN

    override fun afterAll() {
        out.write("Finished".encodeToByteArray())
    }

    override fun onEvent(event: TestResult) {
        val comment = when (val status = event.status) {
            is Pass -> status.message ?: ""
            is Fail -> status.reason
            is Ignored -> status.reason
            is Crash -> status.throwable.message
        }
        out.write(
            "| ${event.resources.first()} | ${event.status} | $comment |"
                .encodeToByteArray()
        )
    }

    override fun onPluginInitialization(plugin: Plugin) {
        out.write("Initializing plugin ${plugin::class.simpleName}".encodeToByteArray())
    }

    override fun onPluginExecutionStart(plugin: Plugin) {
        out.write("Starting plugin ${plugin::class.simpleName}".encodeToByteArray())
        out.write("--------------------------------".encodeToByteArray())
        out.write("| Test name | result | comment |".encodeToByteArray())
    }

    override fun onPluginExecutionEnd(plugin: Plugin) {
        out.write("--------------------------------".encodeToByteArray())
        out.write("Plugin ${plugin::class.simpleName} has completed execution".encodeToByteArray())
    }

    override fun onPluginExecutionError(ex: PluginException) {
        out.write("Error during plugin execution: ${ex.message}".encodeToByteArray())
    }
}
