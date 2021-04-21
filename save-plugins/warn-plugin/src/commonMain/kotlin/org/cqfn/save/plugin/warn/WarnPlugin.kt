package org.cqfn.save.plugin.warn

import okio.FileSystem
import okio.Path
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.plugin.warn.utils.Warning
import org.cqfn.save.plugin.warn.utils.extractWarning

/**
 * A plugin that runs an executable and verifies that it produces required warning messages.
 */
class WarnPlugin : Plugin {
    private val fs = FileSystem.SYSTEM
    private val pb = ProcessBuilder()

    override fun execute(saveProperties: SaveProperties, testConfig: TestConfig) {
        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val warningRegex = warningRegex(warnPluginConfig)
        discoverTestFiles(warnPluginConfig.testResources).forEach { testFile ->
            val expectedWarnings = fs.readLines(testFile)
                .mapNotNull {
                    it.extractWarning(warningRegex, warnPluginConfig.columnCaptureGroup, warnPluginConfig.lineCaptureGroup, warnPluginConfig.messageCaptureGroup)
                }
                .groupBy { it.line to it.column }
                .mapValues { it.value.sortedBy { it.message } }
            val executionResult = pb.exec(warnPluginConfig.execCmd.split(" "), null)
            val actualWarningsMap: Map<Pair<Int?, Int?>, List<Warning>> = executionResult.stdout.mapNotNull {
                with (warnPluginConfig) {
                    it.extractWarning(warningsOutputPattern, columnCaptureGroup, lineCaptureGroup, messageCaptureGroup)
                }
            }
                .groupBy { it.line to it.column }
                .mapValues { it.value.sortedBy { it.message } }
            expectedWarnings.forEach { (pair, warnings) ->
                val actualWarnings = actualWarningsMap[pair]
                requireNotNull(actualWarnings)
                require(warnings.size == actualWarnings.size)
                require(warnings.zip(actualWarnings).all { it.first == it.second })
            }
        }
    }

    internal fun discoverTestFiles(resources: List<Path>) = resources
        .filter { it.name.contains("Test.") }
}
