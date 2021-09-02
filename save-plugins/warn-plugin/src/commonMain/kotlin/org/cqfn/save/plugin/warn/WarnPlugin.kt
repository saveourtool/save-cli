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
import org.cqfn.save.plugin.warn.utils.ExtraFlagsExtractor
import org.cqfn.save.plugin.warn.utils.Warning
import org.cqfn.save.plugin.warn.utils.extractWarning
import org.cqfn.save.plugin.warn.utils.getLineNumber

import okio.FileSystem
import okio.Path

private typealias WarningMap = Map<String, List<Warning>>

/**
 * A plugin that runs an executable and verifies that it produces required warning messages.
 * @property testConfig
 */
class WarnPlugin(
    testConfig: TestConfig,
    testFiles: List<String>,
    fileSystem: FileSystem,
    useInternalRedirections: Boolean = true,
    redirectTo: Path? = null,
) : Plugin(
    testConfig,
    testFiles,
    fileSystem,
    useInternalRedirections,
    redirectTo) {
    private val expectedAndNotReceived = "Some warnings were expected but not received"
    private val unexpected = "Some warnings were unexpected"
    private lateinit var extraFlagsExtractor: ExtraFlagsExtractor

    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()

        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val generalConfig = testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single()
        extraFlagsExtractor = ExtraFlagsExtractor(warnPluginConfig, fs)

        // Special trick to handle cases when tested tool is able to process directories.
        // In this case instead of executing the tool with file names, we execute the tool with directories.
        // 
        // In case, when user doesn't want to use directory mode, he needs simply not to pass [wildCardInDirectoryMode] and it will be null
        return warnPluginConfig.wildCardInDirectoryMode?.let {
            handleTestFile(files.map { it.single() }.toList(), warnPluginConfig, generalConfig).asSequence()
        } ?: run {
            files.chunked(warnPluginConfig.batchSize!!.toInt()).flatMap { chunk ->
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
        val expectedWarnings: WarningMap = paths.associate {
            val warningsForCurrentPath = it.collectWarningsWithLineNumbers(warnPluginConfig, generalConfig)
            it.name to warningsForCurrentPath
        }

        val extraFlagsList = paths.mapNotNull { path ->
            extraFlagsExtractor.extractExtraFlagsFrom(path)
        }
            .distinct()
        require(extraFlagsList.size <= 1) {
            "Extra flags for all files in a batch should be same, but you have batchSize=${warnPluginConfig.batchSize}" +
                    " and there are ${extraFlagsList.size} different sets of flags inside it"
        }
        val extraFlags = extraFlagsList.singleOrNull() ?: ExtraFlags("", "")

        if (expectedWarnings.isEmpty()) {
            logWarn(
                "No expected warnings were found using the following regex pattern:" +
                        " [${generalConfig.expectedWarningsPattern}] in the test files: $paths." +
                        " If you have expected any warnings - please check 'expectedWarningsPattern' or capture groups" +
                        " in your 'save.toml' configuration"
            )
        }

        // joining test files to string with a batchSeparator if the tested tool supports processing of file batches
        // NOTE: save will pass relative paths of Tests (calculated from testRootConfig dir) into the executed tool
        val fileNamesForExecCmd =
                warnPluginConfig.wildCardInDirectoryMode?.let {
                    val directoryPrefix = testConfig
                        .directory
                        .toString()
                        .makeThePathRelativeToTestRoot()
                    // a hack to put only the directory path to the execution command
                    // only in case a directory mode is enabled
                    "$directoryPrefix$it${warnPluginConfig.testNameSuffix}"
                } ?: paths.joinToString(separator = warnPluginConfig.batchSeparator!!) {
                    it.toString().makeThePathRelativeToTestRoot()
                }

        val execFlagsAdjusted = warnPluginConfig.execFlags!!
            .replace("\$${ExtraFlags.keyBefore}", extraFlags.before)
            .replace("\$${ExtraFlags.keyAfter}", extraFlags.after).run {
                if (contains("\$fileName")) {
                    replace("\$fileName", fileNamesForExecCmd)
                } else {
                    plus(" $fileNamesForExecCmd")
                }
            }
        val execCmd = "${generalConfig.execCmd} $execFlagsAdjusted"

        val executionResult = try {
            pb.exec("cd ${testConfig.getRootConfig().location.parent} && $execCmd", redirectTo)
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
                val line = it.getLineNumber(actualWarningsPattern!!, lineCaptureGroupOut)
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

    /**
     * method for getting warnings from test files:
     * 1) reading the file
     * 2) in case of defaultLineMode:
     *     a) calculate real line number
     *     b) get line number from the warning
     * 3) for each line get the warning
     */
    private fun Path.collectWarningsWithLineNumbers(
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig
    ): List<Warning> {
        val linesFile = fs.readLines(this)
        return linesFile.mapIndexed { index, line ->
            val newLine = line.getLineNumber(
                generalConfig.expectedWarningsPattern!!,
                warnPluginConfig.lineCaptureGroup,
                warnPluginConfig.linePlaceholder!!,
                index + 1,
                this,
                linesFile,
            )
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
            .sortedBy { warn -> warn.message }
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
                Pass("$unexpected: $unexpectedWarnings", "$unexpected: ${unexpectedWarnings.size} warnings")
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
