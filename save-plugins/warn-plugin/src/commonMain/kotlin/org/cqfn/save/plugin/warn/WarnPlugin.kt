package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.ExtraFlags
import org.cqfn.save.core.plugin.ExtraFlagsExtractor
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.resolvePlaceholdersFrom
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.ProcessExecutionException
import org.cqfn.save.core.utils.ProcessTimeoutException
import org.cqfn.save.plugin.warn.utils.ResultsChecker
import org.cqfn.save.plugin.warn.utils.Warning
import org.cqfn.save.plugin.warn.utils.extractWarning
import org.cqfn.save.plugin.warn.utils.getLineNumber
import org.cqfn.save.plugin.warn.utils.getLineNumberAndMessage

import okio.FileNotFoundException
import okio.FileSystem
import okio.Path

import kotlin.random.Random

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
    redirectTo
) {
    private lateinit var extraFlagsExtractor: ExtraFlagsExtractor

    override fun handleFiles(files: Sequence<TestFiles>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()
        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val generalConfig = testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single()
        extraFlagsExtractor = ExtraFlagsExtractor(generalConfig, fs)

        // Special trick to handle cases when tested tool is able to process directories.
        // In this case instead of executing the tool with file names, we execute the tool with directories.
        // 
        // In case, when user doesn't want to use directory mode, he needs simply not to pass [wildCardInDirectoryMode] and it will be null
        return warnPluginConfig.wildCardInDirectoryMode?.let {
            handleTestFile(files.map { it.test }.toList(), warnPluginConfig, generalConfig).asSequence()
        } ?: run {
            files.chunked(warnPluginConfig.batchSize!!.toInt()).flatMap { chunk ->
                handleTestFile(chunk.map { it.test }, warnPluginConfig, generalConfig)
            }
        }
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<TestFiles> {
        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val regex = warnPluginConfig.resourceNamePattern
        // returned sequence is a sequence of groups of size 1
        return resourceDirectories.flatMap { directory ->
            fs.list(directory)
                .filter { regex.matches(it.name) }
                .map { Test(it) }
        }
    }

    override fun cleanupTempDir() {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / WarnPlugin::class.simpleName!!)
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
    }

    private fun createTestFiles(paths: List<Path>, warnPluginConfig: WarnPluginConfig): List<Path> {
        val dirName = "${WarnPlugin::class.simpleName!!}-${Random.nextInt()}"
        val dirPath = constructPathForCopyOfTestFile(dirName, paths[0]).parent!!
        createTempDir(dirPath)

        val ignorePatterns = warnPluginConfig.ignoreLinesPatterns

        return paths.map { path ->
            val copyPath = constructPathForCopyOfTestFile(dirName, path)
            fs.write(fs.createFile(copyPath)) {
                fs.readLines(path)
                    .filter { line -> ignorePatterns.none { it.matches(line) } }
                    .map { write((it + "\n").encodeToByteArray()) }
            }
            copyPath
        }
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "SAY_NO_TO_VAR",
        "LongMethod",
        "ReturnCount",
        "SwallowedException",
        "TOO_MANY_LINES_IN_LAMBDA"
    )
    private fun handleTestFile(
        paths: List<Path>,
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig
    ): Sequence<TestResult> {
        // extracting all warnings from test resource files
        val copyPaths: List<Path> = createTestFiles(paths, warnPluginConfig)
        val expectedWarningsMap: WarningMap = copyPaths.associate {
            val warningsForCurrentPath = it.collectWarningsWithLineNumbers(warnPluginConfig, generalConfig)
            it.name to warningsForCurrentPath
        }

        val extraFlagsList = copyPaths.mapNotNull { extraFlagsExtractor.extractExtraFlagsFrom(it) }.distinct()
        require(extraFlagsList.size <= 1) {
            "Extra flags for all files in a batch should be same, but you have batchSize=${warnPluginConfig.batchSize}" +
                    " and there are ${extraFlagsList.size} different sets of flags inside it, namely $extraFlagsList"
        }
        val extraFlags = extraFlagsList.singleOrNull() ?: ExtraFlags("", "")

        if (expectedWarningsMap.isEmpty()) {
            logWarn(
                "No expected warnings were found using the following regex pattern:" +
                        " [${generalConfig.expectedWarningsPattern}] in the test files: $paths." +
                        " If you have expected any warnings - please check 'expectedWarningsPattern' or capture groups" +
                        " in your 'save.toml' configuration"
            )
        }

        // joining test files to string with a batchSeparator if the tested tool supports processing of file batches
        // NOTE: SAVE will pass relative paths of Tests (calculated from testRootConfig dir) into the executed tool
        val fileNamesForExecCmd =
                warnPluginConfig.wildCardInDirectoryMode?.let {
                    // a hack to put only the directory path to the execution command
                    // only in case a directory mode is enabled
                    "${copyPaths[0].parent!!}$it"
                } ?: copyPaths.joinToString(separator = warnPluginConfig.batchSeparator!!)
        val execFlagsAdjusted = resolvePlaceholdersFrom(warnPluginConfig.execFlags, extraFlags, fileNamesForExecCmd)
        val execCmd = "${generalConfig.execCmd} $execFlagsAdjusted"
        val time = generalConfig.timeOutMillis!!.times(copyPaths.size)

        val executionResult = try {
            pb.exec(execCmd, testConfig.getRootConfig().directory.toString(), redirectTo, time)
        } catch (ex: ProcessTimeoutException) {
            logWarn("The following tests took too long to run and were stopped: $paths, timeout for single test: ${ex.timeoutMillis}")
            return failTestResult(paths, ex, execCmd)
        } catch (ex: ProcessExecutionException) {
            return failTestResult(paths, ex, execCmd)
        }

        val stdout =
                warnPluginConfig.testToolResFileOutput?.let {
                    val testToolResFilePath = testConfig.directory / warnPluginConfig.testToolResFileOutput
                    try {
                        fs.readLines(testToolResFilePath)
                    } catch (ex: FileNotFoundException) {
                        logWarn("Trying to read file \"${warnPluginConfig.testToolResFileOutput}\" that was set as an output for a tested tool with testToolResFileOutput," +
                                " but no such file found. Will use the stdout as an input.")
                        executionResult.stdout
                    }
                }
                    ?: run {
                        executionResult.stdout
                    }
        val stderr = executionResult.stderr

        val actualWarningsMap = stdout.mapNotNull {
            with(warnPluginConfig) {
                val line = it.getLineNumber(actualWarningsPattern!!, lineCaptureGroupOut)
                it.extractWarning(
                    actualWarningsPattern,
                    fileNameCaptureGroupOut!!,
                    line,
                    columnCaptureGroupOut,
                    messageCaptureGroupOut!!,
                    benchmarkMode!!,
                )
            }
        }
            .groupBy { it.fileName }
            .mapValues { (_, warning) -> warning.sortedBy { it.message } }

        val resultsChecker = ResultsChecker(
            expectedWarningsMap,
            actualWarningsMap,
            warnPluginConfig,
        )

        return paths.map { path ->
            TestResult(
                Test(path),
                resultsChecker.checkResults(path.name),
                DebugInfo(
                    execCmd,
                    stdout.filter { it.contains(path.name) }.joinToString("\n"),
                    stderr.filter { it.contains(path.name) }.joinToString("\n"),
                    null
                )
            )
        }.asSequence()
    }

    private fun failTestResult(
        paths: List<Path>,
        ex: ProcessExecutionException,
        execCmd: String
    ) = paths.map {
        TestResult(
            Test(it),
            Fail(ex.describe(), ex.describe()),
            DebugInfo(execCmd, null, ex.message, null)
        )
    }.asSequence()

    /**
     * method for getting warnings from test files:
     * 1) reading the file
     * 2) for each line get the warning
     */
    @Suppress("TOO_LONG_FUNCTION")
    private fun Path.collectWarningsWithLineNumbers(
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig
    ): List<Warning> {
        val linesFile = fs.readLines(this)
        return generalConfig.expectedWarningsEndPattern?.let {
            linesFile.mapIndexed { index, line ->
                val newLineAndMessage = line.getLineNumberAndMessage(
                    generalConfig.expectedWarningsPattern!!,
                    generalConfig.expectedWarningsEndPattern!!,
                    generalConfig.expectedWarningsMiddlePattern!!,
                    warnPluginConfig.messageCaptureGroupMiddle!!,
                    warnPluginConfig.messageCaptureGroupEnd!!,
                    warnPluginConfig.lineCaptureGroup,
                    warnPluginConfig.linePlaceholder!!,
                    warnPluginConfig.messageCaptureGroup!!,
                    index + 1,
                    this,
                    linesFile,
                )
                with(warnPluginConfig) {
                    line.extractWarning(
                        generalConfig.expectedWarningsPattern!!,
                        this@collectWarningsWithLineNumbers.name,
                        newLineAndMessage?.first,
                        newLineAndMessage?.second,
                        columnCaptureGroup,
                    )
                }
            }
                .filterNotNull()
                .sortedBy { warn -> warn.message }
        }
            ?: run {
                linesFile.mapIndexed { index, line ->
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
                            benchmarkMode!!,
                        )
                    }
                }
                    .filterNotNull()
                    .sortedBy { warn -> warn.message }
            }
    }
}
