package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.result.TestStatus
import org.cqfn.save.core.utils.ProcessExecutionException
import org.cqfn.save.plugin.warn.utils.Warning
import org.cqfn.save.plugin.warn.utils.extractWarning
import org.cqfn.save.plugin.warn.utils.getLineNumber

import okio.FileSystem
import okio.Path

private typealias WarningMap = MutableMap<String, List<Warning>>

/**
 * A plugin that runs an executable and verifies that it produces required warning messages.
 * @property testConfig
 */
class WarnPlugin(
    testConfig: TestConfig,
    testFiles: List<String>,
    useInternalRedirections: Boolean = true
) : Plugin(
    testConfig,
    testFiles,
    useInternalRedirections
) {
    private val expectedAndNotReceived = "Some warnings were expected but not received"
    private val unexpected = "Some warnings were unexpected"

    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()

        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val generalConfig = testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single()

        // Special trick to handle cases when tested tool is able to process directories.
        // In this case instead of executing the tool with file names, we execute the tool with directories.
        //
        // In case user wants to use directory mode, he needs simply not to pass [wildCardInDirectoryMode] and it will be null
        return warnPluginConfig.wildCardInDirectoryMode?.let {
            handleTestFile(files.map { it.single() }.toList(), warnPluginConfig, generalConfig).asSequence()
        } ?: run {
            files.chunked(warnPluginConfig.batchSize!!).flatMap { chunk ->
                handleTestFile(chunk.map { it.single() }, warnPluginConfig, generalConfig)
            }
        }
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<List<Path>> {
        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val regex = warnPluginConfig.resourceNamePattern
        // returned sequence is a sequence of groups of size 1
        return resourceDirectories.flatMap { directory ->
            fs.list(directory)
                .filter { regex.matches(it.name) }
                .map { listOf(it) }
        }
    }

    override fun cleanupTempDir() {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / WarnPlugin::class.simpleName!!)
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
    }

    private fun plusLine(
        file: Path,
        warningRegex: Regex,
        linesFile: List<String>,
        lineNum: Int
    ): Int {
        var x = 1
        val fileSize = linesFile.size
        while (lineNum - 1 + x < fileSize && warningRegex.find(linesFile[lineNum - 1 + x]) != null) {
            x++
        }
        val newLine = lineNum + x
        if (newLine >= fileSize) {
            logWarn("Some warnings are at the end of the file: <$file>. They will be assigned the following line: $newLine")
        }
        return newLine
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "SAY_NO_TO_VAR",
        "LongMethod"
    )
    private fun handleTestFile(
        paths: List<Path>,
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig
    ): Sequence<TestResult> {
        // extracting all warnings from test resource files
        val expectedWarnings: WarningMap = mutableMapOf()
        paths.forEach {
            val warningsForCurrentPath = it.collectWarningsWithLineNumbers(warnPluginConfig, generalConfig)
            expectedWarnings.putAll(warningsForCurrentPath)
        }

        if (expectedWarnings.isEmpty()) {
            logWarn(
                "No expected warnings were found using the following regex pattern:" +
                        " [${generalConfig.expectedWarningsPattern}] in the test files: $paths." +
                        " If you have expected any warnings - please check 'expectedWarningsPattern' or capture groups" +
                        " in your 'save.toml' configuration"
            )
        }

        // joining test files to string with a batchSeparator if the tested tool supports processing of file batches
        // NOTE: save will pass relative paths of Tests (calculated from tesRootConfig dir) into the executed tool
        val fileNamesForExecCmd =
            warnPluginConfig.wildCardInDirectoryMode?.let {
                val directoryPrefix = testConfig
                    .directory
                    .toString()
                    .makeThePathRelativeToTestRoot()
                // a hack to put only the directory path to the execution command
                // only in case a directory mode is enabled
                "$directoryPrefix${it}${warnPluginConfig.testNameSuffix}"
            } ?: run {
                paths.joinToString(separator = warnPluginConfig.batchSeparator!!) {
                    it.toString().makeThePathRelativeToTestRoot()
                }
            }

        val execCmd = "${generalConfig.execCmd} ${warnPluginConfig.execFlags} $fileNamesForExecCmd"

        val executionResult = try {
            pb.exec("cd ${testConfig.getRootConfig().location.parent} && " + execCmd, null)
        } catch (ex: ProcessExecutionException) {
            return sequenceOf(
                TestResult(
                    paths,
                    Fail(ex.describe(), ex.describe()),
                    DebugInfo(null, ex.message, null)
                )
            )
        }
        val stdout = executionResult.stdout
        val stderr = executionResult.stderr

        val actualWarningsMap = executionResult.stdout.mapNotNull {
            with(warnPluginConfig) {
                val line = it.getLineNumber(actualWarningsPattern!!, lineCaptureGroupOut, linePlaceholder!!, null)
                it.extractWarning(
                    actualWarningsPattern,
                    fileNameCaptureGroupOut!!,
                    line,
                    columnCaptureGroupOut,
                    messageCaptureGroupOut!!
                )
            }
        }
            .groupBy { it.fileName }
            .mapValues { (_, warning) -> warning.sortedBy { it.message } }

        return paths.map { path ->
            TestResult(
                listOf(path),
                checkResults(
                    expectedWarnings[path.name] ?: listOf(),
                    actualWarningsMap[path.name] ?: listOf(),
                    warnPluginConfig
                ),
                DebugInfo(
                    stdout.filter { it.contains(path.name) }.joinToString("\n"),
                    stderr.filter { it.contains(path.name) }.joinToString("\n"),
                    null
                )
            )
        }.asSequence()
    }

    private fun String.makeThePathRelativeToTestRoot() =
        this.replace("${testConfig.getRootConfig().directory}", "")
            .trimStart('/', '\\')

    private fun Path.collectWarningsWithLineNumbers(
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig
    ): WarningMap {
        val linesFile = fs.readLines(this)
        return linesFile.mapIndexed { index, line ->
            val newLine = if (warnPluginConfig.defaultLineMode!!) {
                plusLine(this, generalConfig.expectedWarningsPattern!!, linesFile, index)
            } else {
                line.getLineNumber(
                    generalConfig.expectedWarningsPattern!!,
                    warnPluginConfig.lineCaptureGroup,
                    warnPluginConfig.linePlaceholder!!,
                    index
                )
            }
            with(warnPluginConfig) {
                line.extractWarning(
                    generalConfig.expectedWarningsPattern!!,
                    this@collectWarningsWithLineNumbers.name,
                    newLine,
                    columnCaptureGroup,
                    messageCaptureGroup!!,
                )
            }
        }
            .filterNotNull()
            .groupBy {
                it.fileName
            }
            .mapValues { it.value.sortedBy { warn -> warn.message } }
            .toMutableMap()
    }

    /**
     * Compares actual and expected warnings and returns TestResult
     *
     * @param expectedWarningsMap expected warnings, grouped by LineCol
     * @param actualWarningsMap actual warnings, grouped by LineCol
     * @param warnPluginConfig configuration of warn plugin
     * @return [TestResult]
     */
    @Suppress("TYPE_ALIAS")
    internal fun checkResults(
        expectedWarningsMap: List<Warning>,
        actualWarningsMap: List<Warning>,
        warnPluginConfig: WarnPluginConfig
    ): TestStatus {
        val missingWarnings = expectedWarningsMap - actualWarningsMap
        val unexpectedWarnings = actualWarningsMap - expectedWarningsMap

        return when (missingWarnings.isEmpty() to unexpectedWarnings.isEmpty()) {
            false to true -> createFail(expectedAndNotReceived, missingWarnings)
            false to false -> Fail(
                "$expectedAndNotReceived: $missingWarnings, and ${unexpected.lowercase()}: $unexpectedWarnings",
                "$expectedAndNotReceived (${missingWarnings.size}), and ${unexpected.lowercase()} (${unexpectedWarnings.size})"
            )
            true to false -> if (warnPluginConfig.exactWarningsMatch == false) {
                Pass("$unexpected: $unexpectedWarnings")
            } else {
                createFail(unexpected, unexpectedWarnings)
            }
            true to true -> Pass(null)
            else -> Fail("", "")
        }
    }

    private fun createFail(baseText: String, warnings: List<Warning>) =
        Fail("$baseText: $warnings", "$baseText (${warnings.size})")
}
