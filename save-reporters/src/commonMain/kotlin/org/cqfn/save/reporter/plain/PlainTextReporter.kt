package org.cqfn.save.reporter.plain

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
class PlainTextReporter(override val out: BufferedSink) : Reporter {
    override val type: ReportType = ReportType.PLAIN

    override fun beforeAll() {
        logDebug("Initializing reporter ${this::class.qualifiedName} of type $type\n")
    }

    override fun afterAll() {
        logDebug("Finished reporter ${this::class.qualifiedName} of type $type\n")
    }

    override fun onSuiteStart(suiteName: String) {
        out.write("Test suite [$suiteName]\n".encodeToByteArray())
    }

    override fun onSuiteEnd(suiteName: String) {
        out.write("Completed test suite [$suiteName]\n".encodeToByteArray())
    }

    override fun onEvent(event: TestResult) {
        val comment: String = when (val status = event.status) {
            is Pass -> status.message ?: ""
            is Fail -> status.shortReason
            is Ignored -> status.reason
            is Crash -> status.throwable.describe()
        }
        val shortComment = comment.lines().let { lines ->
            lines.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.plus(
                    if (lines.size > 1) "..." else ""
                )
        }
            ?: ""
        out.write(
            "| ${event.resources.first()} | ${event.status::class.simpleName} | $shortComment |\n"
                .encodeToByteArray()
        )
    }

    override fun onPluginInitialization(plugin: Plugin) {
        out.write("Initializing plugin ${plugin::class.simpleName}\n".encodeToByteArray())
    }

    override fun onPluginExecutionStart(plugin: Plugin) {
        out.write("Executing plugin ${plugin::class.simpleName}\n".encodeToByteArray())
        out.write("--------------------------------\n".encodeToByteArray())
        out.write("| Test name | result | comment |\n".encodeToByteArray())
    }

    override fun onPluginExecutionSkip(plugin: Plugin) {
        out.write("--------------------------------\n".encodeToByteArray())
        out.write("Plugin ${plugin::class.simpleName} has been skipped\n".encodeToByteArray())
    }

    override fun onPluginExecutionEnd(plugin: Plugin) {
        out.write("--------------------------------\n".encodeToByteArray())
        out.write("Plugin ${plugin::class.simpleName} has completed execution\n".encodeToByteArray())
    }

    override fun onPluginExecutionError(ex: PluginException) {
        out.write("Error during plugin execution: ${ex.describe()}\n".encodeToByteArray())
    }
}
