package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.result.TestStatus
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.plugin.warn.WarnPluginConfig.Companion.defaultResourceNamePattern
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

    override fun execute(testConfig: TestConfig): Sequence<TestResult> {
        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        return discoverTestFiles(testConfig.directory).map { resources ->
            handleTestFile(resources.single(), warnPluginConfig)
        }
    }

    override fun discoverTestFiles(root: Path) = root
        .resourceDirectories()
        .map { directory ->
            FileSystem.SYSTEM.list(directory)
                .filter { defaultResourceNamePattern.matches(it.name) }
        }
        .asSequence()
        .map { listOf(it) }
        .flatten()

    @Suppress("UnusedPrivateMember")
    private fun handleTestFile(
        path: Path,
        warnPluginConfig: WarnPluginConfig): TestResult {
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
        val executionResult = pb.exec(warnPluginConfig.execCmd, null)
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
                             actualWarningsMap: Map<LineColumn?, List<Warning>>): TestStatus =
            checkCollectionsDiffer(expectedWarningsMap, actualWarningsMap)?.let { message ->
                Fail(message)
            }
                ?: Pass

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
