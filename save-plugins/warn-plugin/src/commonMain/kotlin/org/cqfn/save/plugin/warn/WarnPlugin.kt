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
        checkCollectionsDiffer(expectedWarningsMap, actualWarningsMap)?.let { message ->
            return Fail(message)
        }
        checkWarningsDiffer(expectedWarningsMap, actualWarningsMap)?.let { message ->
            return Fail(message)
        }
        return Pass
    }

    @Suppress("TYPE_ALIAS")
    private fun checkCollectionsDiffer(expectedWarningsMap: Map<LineColumn?, List<Warning>>,
                                       actualWarningsMap: Map<LineColumn?, List<Warning>>): String? {
        val missingWarnings = expectedWarningsMap.filterValues { it !in actualWarningsMap.values }
        val unexpectedWarnings = actualWarningsMap.filterValues { it !in expectedWarningsMap.values }
        return when {
            missingWarnings.isNotEmpty() && unexpectedWarnings.isEmpty() -> "Some warnings were expected but not received: ${missingWarnings.values}"
            missingWarnings.isNotEmpty() && unexpectedWarnings.isNotEmpty() -> "Some warnings were expected but not received: ${missingWarnings.values}, " +
                    "and others were unexpected: ${unexpectedWarnings.values}"
            missingWarnings.isEmpty() && unexpectedWarnings.isNotEmpty() -> "Some warnings were unexpected: ${unexpectedWarnings.values}"
            else -> null
        }
    }

    @Suppress("TYPE_ALIAS")
    private fun checkWarningsDiffer(expectedWarningsMap: Map<LineColumn?, List<Warning>>,
                                    actualWarningsMap: Map<LineColumn?, List<Warning>>): String? = expectedWarningsMap.mapNotNull { (pair, warnings) ->
        val actualWarnings = actualWarningsMap[pair]
        when {
            actualWarnings == null -> "Expected a warning at $pair, but it was not present in actual output"
            warnings.size != actualWarnings.size -> "Number of expected and actual warnings differ at $pair: expected ${warnings.size} but was ${actualWarnings.size}"
            else -> warnings.zip(actualWarnings).filter {
                it.first != it.second
            }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ", prefix = "Warnings differ: ") { "expected [${it.first}] but was [${it.second}]" }
        }
    }
        .joinToString(", ")
        .takeIf { it.isNotEmpty() }
}
