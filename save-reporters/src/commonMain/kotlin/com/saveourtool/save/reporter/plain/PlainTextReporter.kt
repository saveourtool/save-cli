package com.saveourtool.save.reporter.plain

import com.saveourtool.save.core.config.ReportType
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.plugin.PluginException
import com.saveourtool.save.core.reporter.Reporter
import com.saveourtool.save.core.reporter.Statistics
import com.saveourtool.save.core.result.Crash
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestResult

import okio.BufferedSink

/**
 * Reporter that writes execution logs in plain text.
 *
 * @property out a sink for output
 */
@Suppress("WRONG_INDENTATION")  // https://github.com/saveourtool/diktat/issues/1490
open class PlainTextReporter(override val out: BufferedSink) : Reporter {
    override val type: ReportType = ReportType.PLAIN
    private var currentTestSuite: String? = null
    private var currentPlugin: String? = null
    private val statistics = Statistics()

    override fun beforeAll() {
        logDebug("Initializing reporter ${this::class.qualifiedName} of type $type")
        val headers = listOf("Test suite", "Plugin", "Test", "result", "comment")
        out.write("--------------------------------\n".encodeToByteArray())
        out.write(headers.joinToString(prefix = "| ", separator = " | ", postfix = " |\n").encodeToByteArray())
        out.write(headers.joinToString(prefix = "| ", separator = " | ", postfix = " |\n") { "------" }.encodeToByteArray())
    }

    @Suppress("MAGIC_NUMBER", "MagicNumber")
    override fun afterAll() {
        out.write("--------------------------------\n".encodeToByteArray())
        with(statistics) {
            val passRate = if (total > 0) (passed.toFloat() / total * 100).toInt() else 0
            val status = if (passed == total) "SUCCESS" else "FAILED"
            // `%` should be escaped as `%%` for correct printing
            out.write(
                "$status: $total tests, $passRate%% successful, failed: $failed, skipped: $skipped"
                    .encodeToByteArray()
            )
        }
        logDebug("Finished reporter ${this::class.qualifiedName} of type $type")
    }

    override fun onSuiteStart(suiteName: String) {
        currentTestSuite = suiteName
    }

    override fun onSuiteEnd(suiteName: String) {
        currentTestSuite = null
    }

    override fun onEvent(event: TestResult) {
        statistics.updateFrom(event)
        val comment: String = when (val status = event.status) {
            is Pass -> status.shortMessage ?: ""
            is Fail -> status.shortReason
            is Ignored -> status.reason
            is Crash -> status.description
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
            "| $currentTestSuite | $currentPlugin | ${event.resources.test} | ${event.status::class.simpleName} | $shortComment |\n"
                .encodeToByteArray()
        )
    }

    override fun onPluginInitialization(plugin: Plugin) {
        currentPlugin = plugin::class.simpleName
    }

    override fun onPluginExecutionStart(plugin: Plugin) = Unit

    override fun onPluginExecutionEnd(plugin: Plugin) {
        currentPlugin = null
    }

    override fun onPluginExecutionError(ex: PluginException) {
        out.write("| $currentTestSuite | $currentPlugin | | Error | Error during plugin execution: ${ex.describe()}\n".encodeToByteArray())
    }
}
