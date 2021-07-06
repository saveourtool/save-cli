package org.cqfn.save.reporter.json

import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.core.reporter.Reporter
import org.cqfn.save.core.result.TestResult

import okio.BufferedSink

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Reporter that produces a JSON report as a [Report]
 *
 * @property out a sink for output
 */
class JsonReporter(override val out: BufferedSink) : Reporter {
    override val type: ReportType = ReportType.JSON

    /**
     * Formatter to serialize/deserialize to JSON
     */
    internal val json = Json

    private var isFirstEvent = true  // todo: use AtomicBoolean

    private var isFirstSuite = true  // todo: use AtomicBoolean

    override fun beforeAll() {
        out.write("[\n".encodeToByteArray())
    }

    override fun afterAll() {
        out.write("]\n".encodeToByteArray())
    }

    override fun onSuiteStart(suiteName: String) {
        if (!isFirstSuite) {
            out.write(",\n".encodeToByteArray())
        } else {
            isFirstSuite = false
        }
        out.write("{\n\"testSuite\": \"$suiteName\",\n\"pluginExecutions\":\n[\n".encodeToByteArray())
    }

    override fun onSuiteEnd(suiteName: String) {
        out.write("]\n}\n".encodeToByteArray())
    }

    override fun onEvent(event: TestResult) {
        if (!isFirstEvent) {
            out.write(",".encodeToByteArray())
        } else {
            isFirstEvent = false
        }
        out.write("${json.encodeToString(event)}\n".encodeToByteArray())
    }

    override fun onPluginInitialization(plugin: Plugin) {
        out.write("{\n\"plugin\": \"${plugin::class.simpleName}\",\n".encodeToByteArray())
    }

    override fun onPluginExecutionStart(plugin: Plugin) {
        isFirstEvent = true
        out.write("\"testResults\": [\n".encodeToByteArray())
    }

    override fun onPluginExecutionEnd(plugin: Plugin) {
        out.write("]}\n".encodeToByteArray())
    }

    override fun onPluginExecutionError(ex: PluginException) {
        if (!isFirstEvent) {
            out.write(",".encodeToByteArray())
        } else {
            isFirstEvent = false
        }
        out.write("${Json.encodeToString(ex)}\n".encodeToByteArray())
    }
}
