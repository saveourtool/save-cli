package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.plugin.warn.utils.extractWarning

import okio.FileSystem
import okio.Path

/**
 * A plugin that runs an executable and verifies that it produces required warning messages.
 */
class WarnPlugin : Plugin {
    private val fs = FileSystem.SYSTEM
    private val pb = ProcessBuilder()

    override fun execute(saveProperties: SaveProperties, testConfig: TestConfig) {
        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        discoverTestFiles(warnPluginConfig.testResources).forEach { testFile ->
            handleTestFile(testFile, warnPluginConfig, saveProperties)
        }
    }

    /**
     * Discover test resources for warn-plugin among [resources]
     *
     * @param resources a collection of files
     */
    internal fun discoverTestFiles(resources: List<Path>) = resources
        .filter { it.name.contains("Test.") }

    private fun handleTestFile(
        path: Path,
        warnPluginConfig: WarnPluginConfig,
        saveProperties: SaveProperties) {
        val expectedWarnings = fs.readLines(path)
            .mapNotNull {
                with(warnPluginConfig) {
                    it.extractWarning(
                        warningsInputPattern,
                        columnCaptureGroup,
                        lineCaptureGroup,
                        messageCaptureGroup
                    )
                }
            }
            .mapIndexed { index, warning ->
                if (saveProperties.ignoreSaveComments!! && warning?.line != null) warning.copy(line = warning.line + index + 1) else warning
            }
            .groupBy { it.line to it.column }
            .mapValues { it.value.sortedBy { it.message } }
        val executionResult = pb.exec(warnPluginConfig.execCmd.split(" "), null)
        val actualWarningsMap = executionResult.stdout.mapNotNull {
            with(warnPluginConfig) {
                it.extractWarning(warningsOutputPattern, columnCaptureGroup, lineCaptureGroup, messageCaptureGroup)
            }
        }
            .groupBy { it.line to it.column }
            .mapValues { it.value.sortedBy { it.message } }
        // todo: handle test results here
        require(expectedWarnings.size == actualWarningsMap.size)
        expectedWarnings.forEach { (pair, warnings) ->
            val actualWarnings = actualWarningsMap[pair]
            requireNotNull(actualWarnings)
            require(warnings.size == actualWarnings.size)
            require(warnings.zip(actualWarnings).all { it.first == it.second })
        }
    }
}
