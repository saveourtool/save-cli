package org.cqfn.save.reporter.json

import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.core.reporter.Reporter
import org.cqfn.save.core.result.TestResult

import okio.BufferedSink

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Reporter that produces a JSON report as a [Report]
 *
 * @property out a sink for output
 *
 * @param builder additional configuration lambda for serializers module
 */
class JsonReporter(
    override val out: BufferedSink,
    builder: PolymorphicModuleBuilder<Plugin.TestFiles>.() -> Unit = {}
) : Reporter {
    override val type: ReportType = ReportType.JSON

    /**
     * Formatter to serialize/deserialize to JSON
     */
    val json: Json = Json {
        serializersModule = SerializersModule {
            polymorphic(Plugin.TestFiles::class) {
                subclass(Plugin.Test::class)
                builder()
            }
        }
    }

    private var isFirstEvent = true  // todo: use AtomicBoolean

    private var isFirstSuite = true  // todo: use AtomicBoolean

    override fun beforeAll() {
        out.write("[\n".encodeToByteArray())
    }

    override fun afterAll() {
        out.write("]\n".encodeToByteArray())
    }

    override fun onSuiteStart(suiteName: String) {
        isFirstSuite = out.appendCommaUnless(isFirstSuite)
        out.write("{\n\"testSuite\": \"$suiteName\",\n\"pluginExecutions\":\n[\n".encodeToByteArray())
    }

    override fun onSuiteEnd(suiteName: String) {
        out.write("]\n}\n".encodeToByteArray())
    }

    override fun onEvent(event: TestResult) {
        isFirstEvent = out.appendCommaUnless(isFirstEvent)
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
        isFirstEvent = out.appendCommaUnless(isFirstEvent)
        out.write("${Json.encodeToString(ex)}\n".encodeToByteArray())
    }

    /**
     * Writes a comma into [this] [BufferedSink], if [condition] is `false`.
     *
     * @return `false` after the invocation
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    private fun BufferedSink.appendCommaUnless(condition: Boolean): Boolean {
        if (!condition) {
            write(",".encodeToByteArray())
        }
        return false
    }
}
