package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.result.TestStatus
import org.cqfn.save.core.utils.AtomicInt
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.plugin.warn.utils.Warning
import org.cqfn.save.plugin.warn.utils.extractWarning

import okio.FileSystem
import okio.Path

private typealias LineColumn = Pair<Int, Int>

/**
 * A plugin that runs an executable and verifies that it produces required warning messages.
 * @property testConfig
 */
class WarnPlugin(
    testConfig: TestConfig,
    testFiles: List<String> = emptyList(),
    useInternalRedirections: Boolean = true) : Plugin(
    testConfig,
    testFiles,
    useInternalRedirections) {
    private val fs = FileSystem.SYSTEM
    private val pb = ProcessBuilder()

    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        val flattenedResources = files.toList().flatten()
        if (flattenedResources.isEmpty()) {
            return emptySequence()
        }
        logInfo("Discovered the following test resources: $flattenedResources")

        testConfig.validateAndSetDefaults()

        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val generalConfig = testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().singleOrNull()

        return files.map { resources ->
            handleTestFile(resources.single(), warnPluginConfig, generalConfig)
        }
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<List<Path>> {
        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val regex = warnPluginConfig.resourceNamePattern
        return resourceDirectories
            .map { directory ->
                FileSystem.SYSTEM.list(directory)
                    .filter { (regex).matches(it.name) }
            }
            .filter { it.isNotEmpty() }
    }

    override fun cleanupTempDir() {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / WarnPlugin::class.simpleName!!)
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
    }

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
                        warningsInputPattern!!,
                        columnCaptureGroup,
                        lineCaptureGroup,
                        messageCaptureGroup!!
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

        val execCmd: String = if (generalConfig!!.ignoreSaveComments == true) {
            val fileName = createTestFile(path, warnPluginConfig.warningsInputPattern!!)
            "${generalConfig.execCmd} ${warnPluginConfig.execFlags} $fileName"
        } else {
            "${(generalConfig.execCmd)} ${warnPluginConfig.execFlags} $path"
        }

        val executionResult = pb.exec(execCmd, null, useInternalRedirections)
        val stdout = executionResult.stdout.joinToString("\n")
        val stderr = executionResult.stderr.joinToString("\n")
        val status = if (executionResult.code != 0) Fail(stderr) else null
        val actualWarningsMap = executionResult.stdout.mapNotNull {
            with(warnPluginConfig) {
                it.extractWarning(warningsOutputPattern!!, columnCaptureGroup, lineCaptureGroup, messageCaptureGroup!!)
            }
        }
            .groupBy { if (it.line != null && it.column != null) it.line to it.column else null }
            .mapValues { (_, warning) -> warning.sortedBy { it.message } }
        return TestResult(
            listOf(path),
            status ?: checkResults(expectedWarnings, actualWarningsMap, warnPluginConfig),
            DebugInfo(stdout, stderr, null)
        )
    }

    /**
     * @param path
     * @param warningsInputPattern
     * @return name of the temporary file
     */
    internal fun createTestFile(path: Path, warningsInputPattern: Regex): String {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / WarnPlugin::class.simpleName!!)

        createTempDir(tmpDir)

        val fileName = tmpDir / testFileName()
        fs.write(fs.createFile(fileName)) {
            fs.readLines(path).forEach {
                if (!warningsInputPattern.matches(it)) {
                    write(
                        (it + "\n").encodeToByteArray()
                    )
                }
            }
        }
        return fileName.toString()
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
