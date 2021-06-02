package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.result.TestStatus
import org.cqfn.save.core.utils.AtomicInt
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.plugin.warn.WarnPluginConfig.Companion.defaultResourceNamePattern
import org.cqfn.save.plugin.warn.WarnPluginConfig.Companion.defaultInputPattern
import org.cqfn.save.plugin.warn.WarnPluginConfig.Companion.defaultOutputPattern
import org.cqfn.save.plugin.warn.utils.Warning
import org.cqfn.save.plugin.warn.utils.extractWarning

import okio.FileSystem
import okio.Path

private typealias LineColumn = Pair<Int, Int>

/**
 * A plugin that runs an executable and verifies that it produces required warning messages.
 * @property testConfig
 */
class WarnPlugin(testConfig: TestConfig, testFiles: List<String> = emptyList()) : Plugin(testConfig, testFiles) {
    private val fs = FileSystem.SYSTEM
    private val pb = ProcessBuilder()

    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        val flattenedResources = files.toList().flatten()
        if (flattenedResources.isEmpty()) {
            logWarn("No resources discovered for WarnPlugin in [${testConfig.location}]")
        } else {
            logInfo("Discovered the following test resources: $flattenedResources")
        }

        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val generalConfig = testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().singleOrNull()

        return discoverTestFiles(testConfig.directory).map { resources ->
            handleTestFile(resources.single(), warnPluginConfig, generalConfig)
        }
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<List<Path>> = resourceDirectories
        .map { directory ->
            FileSystem.SYSTEM.list(directory)
                .filter { defaultResourceNamePattern.matches(it.name) }
        }
        .filter { it.isNotEmpty() }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "SAY_NO_TO_VAR")
    private fun handleTestFile(
        path: Path,
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig?): TestResult {
        val expectedWarnings = fs.readLines(path)
            .mapNotNull {
                with(warnPluginConfig) {
                    it.extractWarning(
                        warningsInputPattern ?: defaultInputPattern,
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

        val execCmd: String = if (generalConfig?.ignoreSaveComments == true) {
            val fileName = createTestFile(path, warnPluginConfig.warningsInputPattern ?: defaultInputPattern)
            warnPluginConfig.execCmd + " $fileName"
        } else {
            warnPluginConfig.execCmd + " ${path.name}"
        }

        val executionResult = pb.exec(execCmd, null)
        val actualWarningsMap = executionResult.stdout.mapNotNull {
            with(warnPluginConfig) {
                it.extractWarning(warningsOutputPattern ?: defaultOutputPattern, columnCaptureGroup, lineCaptureGroup, messageCaptureGroup)
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
     * @param warningsInputPattern
     * @return name of the temporary file
     */
    internal fun createTestFile(path: Path, warningsInputPattern: Regex): String {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / WarnPlugin::class.simpleName!!)

        if (fs.exists(tmpDir) && atomicInt.get() == 0) {
            fs.deleteRecursively(tmpDir)
            fs.createDirectory(tmpDir)
        } else if (!fs.exists(tmpDir)) {
            fs.createDirectory(tmpDir)
        }

        val fileName = testFileName()
        fs.write(fs.createFile(tmpDir / fileName)) {
            fs.readLines(path).forEach {
                if (!warningsInputPattern.matches(it)) {
                    write(
                        (it + "\n").encodeToByteArray()
                    )
                }
            }
        }
        return fileName
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
            true to false -> if (warnPluginConfig.exactWarningsMatch == false) {
                Pass("Some warnings were unexpected: $unexpectedWarnings")
            } else {
                Fail("Some warnings were unexpected: $unexpectedWarnings")
            }
            true to true -> Pass(null)
            else -> Fail("")
        }
    }

    private fun testFileName(): String = "test_file${atomicInt.addAndGet(1)}"

    companion object {
        val atomicInt: AtomicInt = AtomicInt(0)
    }
}
