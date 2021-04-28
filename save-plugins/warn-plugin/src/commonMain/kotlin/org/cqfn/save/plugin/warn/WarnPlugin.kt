package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.result.TestStatus
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.plugin.warn.utils.Warning
import org.cqfn.save.plugin.warn.utils.extractWarning

import okio.FileSystem
import okio.Path

private typealias LineColumn = Pair<Int, Int>

/**
 * A plugin that runs an executable and verifies that it produces required warning messages.
 */
class WarnPlugin : Plugin {
    private val fs = FileSystem.SYSTEM
    private val pb = ProcessBuilder()

    override fun execute(saveProperties: SaveProperties, testConfig: TestConfig): Sequence<TestResult> {
        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        return sequence {
            discoverTestFiles(warnPluginConfig.testResources).forEach { testFile ->
                yield(handleTestFile(testFile, warnPluginConfig, saveProperties))
            }
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
        saveProperties: SaveProperties): TestResult {
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
            .groupBy {
                if (it.line != null && it.column != null) {
                    it.line to it.column
                } else {
                    null
                }
            }
            .mapValues { it.value.sortedBy { it.message } }
        // todo: create a temp file with technical comments removed and feed it to the tool
        // todo: do not split the command; change this after https://github.com/cqfn/save/pull/41
        val executionResult = pb.exec(warnPluginConfig.execCmd.split(" "), null)
        val actualWarningsMap = executionResult.stdout.mapNotNull {
            with(warnPluginConfig) {
                it.extractWarning(warningsOutputPattern, columnCaptureGroup, lineCaptureGroup, messageCaptureGroup)
            }
        }
            .groupBy { if (it.line != null && it.column != null) it.line to it.column else null }
            .mapValues { (_, warning) -> warning.sortedBy { it.message } }
        return TestResult(
            listOf(path),
            checkResults(expectedWarnings, actualWarningsMap),
            DebugInfo(executionResult.stdout.joinToString("\n"), executionResult.stderr.joinToString("\n"), null)
        )
    }

    @Suppress("TYPE_ALIAS")
    private fun checkResults(expectedWarningsMap: Map<LineColumn?, List<Warning>>,
                             actualWarningsMap: Map<LineColumn?, List<Warning>>): TestStatus {
        return checkCollectionsDiffer(expectedWarningsMap, actualWarningsMap)?.let { message ->
            Fail(message)
        }
            ?: Pass
    }

    @Suppress("TYPE_ALIAS")
    private fun checkCollectionsDiffer(expectedWarningsMap: Map<LineColumn?, List<Warning>>,
                                       actualWarningsMap: Map<LineColumn?, List<Warning>>): String? {
        val missingWarnings = expectedWarningsMap.filterValues { it !in actualWarningsMap.values }.values
        val unexpectedWarnings = actualWarningsMap.filterValues { it !in expectedWarningsMap.values }.values
        return when (missingWarnings.isEmpty() to unexpectedWarnings.isEmpty()) {
            false to true -> "Some warnings were expected but not received: $missingWarnings"
            false to false -> "Some warnings were expected but not received: $missingWarnings, " +
                    "and others were unexpected: $unexpectedWarnings"
            true to false -> "Some warnings were unexpected: $unexpectedWarnings"
            true to true -> null
            else -> ""
        }
    }
}
