package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.findAllFilesMatching
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.plugin.GeneralConfig
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
 * @property testConfig
 */
class WarnPlugin(testConfig: TestConfig) : Plugin(testConfig) {
    private val fs = FileSystem.SYSTEM
    private val pb = ProcessBuilder()

    override fun execute(): Sequence<TestResult> {
        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val generalConfig = testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single()
        return discoverTestFiles(testConfig.directory).map { resources ->
            handleTestFile(resources.single(), warnPluginConfig, generalConfig)
        }
    }

    override fun discoverTestFiles(root: Path) = root
        .resourceDirectories()
        .map { directory ->
            FileSystem.SYSTEM.list(directory)
                .filter { defaultResourceNamePattern.matches(it.name) }
        }
        .filter { it.isNotEmpty() }

    @Suppress(
        "UnusedPrivateMember",
        "TOO_LONG_FUNCTION",
        "SAY_NO_TO_VAR")
    private fun handleTestFile(
        path: Path,
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig): TestResult {
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

        var execCmd: String = warnPluginConfig.execCmd

        if (generalConfig.ignoreSaveComments) {
            createTestFile(path)
            execCmd = warnPluginConfig.execCmd + " test_file"
        }

        val executionResult = pb.exec(execCmd, null)
        val actualWarningsMap = executionResult.stdout.mapNotNull {
            with(warnPluginConfig) {
                it.extractWarning(warningsOutputPattern, columnCaptureGroup, lineCaptureGroup, messageCaptureGroup)
            }
        }
            .groupBy { if (it.line != null && it.column != null) it.line to it.column else null }
            .mapValues { (_, warning) -> warning.sortedBy { it.message } }
        return TestResult(
            listOf(path),
            checkResults(expectedWarnings, actualWarningsMap, warnPluginConfig),
            DebugInfo(executionResult.stdout.joinToString("\n"), executionResult.stderr.joinToString("\n"), null)
        )
    }

    /**
     * @param path
     */
    internal fun createTestFile(path: Path) {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / WarnPlugin::class.simpleName!!)

        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
        fs.createDirectory(tmpDir)

        fs.write(fs.createFile(tmpDir / "test_file")) {
            fs.readLines(path).forEach {
                if (!it.contains("// ;warn:")) {
                    write(
                        (it + "\n").encodeToByteArray()
                    )
                }
            }
        }
    }

    @Suppress("TYPE_ALIAS")
    private fun checkResults(expectedWarningsMap: Map<LineColumn?, List<Warning>>,
                             actualWarningsMap: Map<LineColumn?, List<Warning>>,
                             warnPluginConfig: WarnPluginConfig): TestStatus {
        val missingWarnings = expectedWarningsMap.filterValues { it !in actualWarningsMap.values }.values
        val unexpectedWarnings = actualWarningsMap.filterValues { it !in expectedWarningsMap.values }.values

        return when (missingWarnings.isEmpty() to unexpectedWarnings.isEmpty()) {
            false to true -> Fail("Some warnings were expected but not received: $missingWarnings")
            false to false -> Fail("Some warnings were expected but not received: $missingWarnings, " +
                    "and others were unexpected: $unexpectedWarnings")
            true to false -> if (!warnPluginConfig.exactWarningsMatch) {
                Pass("Some warnings were unexpected: $unexpectedWarnings")
            } else {
                Fail("Some warnings were unexpected: $unexpectedWarnings")
            }
            true to true -> Pass(null)
            else -> Fail("")
        }
    }
}
