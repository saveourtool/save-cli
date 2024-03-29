package com.saveourtool.save.plugin.warn

import com.saveourtool.save.core.config.ActualWarningsFormat
import com.saveourtool.save.core.config.ExpectedWarningsFormat
import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.createFile
import com.saveourtool.save.core.files.getWorkingDirectory
import com.saveourtool.save.core.files.readLines
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logTrace
import com.saveourtool.save.core.logging.logWarn
import com.saveourtool.save.core.plugin.ExtraFlagsExtractor
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.result.DebugInfo
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.TestResult
import com.saveourtool.save.core.utils.ExecutionResult
import com.saveourtool.save.core.utils.ProcessExecutionException
import com.saveourtool.save.core.utils.ProcessTimeoutException
import com.saveourtool.save.core.utils.SarifParsingException
import com.saveourtool.save.core.utils.calculatePathToSarifFile
import com.saveourtool.save.core.utils.singleIsInstance
import com.saveourtool.save.plugin.warn.sarif.toWarnings
import com.saveourtool.save.plugin.warn.utils.CmdExecutorWarn
import com.saveourtool.save.plugin.warn.utils.ResultsChecker
import com.saveourtool.save.plugin.warn.utils.Warning
import com.saveourtool.save.plugin.warn.utils.collectWarningsFromPlain
import com.saveourtool.save.plugin.warn.utils.collectWarningsFromSarif
import com.saveourtool.save.plugin.warn.utils.collectionMultilineWarnings
import com.saveourtool.save.plugin.warn.utils.collectionSingleWarnings
import com.saveourtool.save.plugin.warn.utils.extractWarning

import io.github.detekt.sarif4k.SarifSchema210
import okio.FileSystem
import okio.Path

import kotlin.random.Random
import kotlinx.serialization.json.Json

private typealias WarningMap = Map<String, List<Warning>>

/**
 * A plugin that runs an executable and verifies that it produces required warning messages.
 * @property testConfig
 */
@Suppress("TooManyFunctions")
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
    private lateinit var tmpDirName: String

    override fun handleFiles(files: Sequence<TestFiles>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()
        val warnPluginConfig: WarnPluginConfig = testConfig.pluginConfigs.singleIsInstance()
        val generalConfig: GeneralConfig = testConfig.pluginConfigs.singleIsInstance()
        val batchSize = requireNotNull(generalConfig.batchSize) {
            "`batchSize` is not set"
        }.toInt()
        val batchSeparator = requireNotNull(generalConfig.batchSeparator) {
            "`batchSeparator` is not set"
        }
        extraFlagsExtractor = ExtraFlagsExtractor(generalConfig, fs)

        // Special trick to handle cases when tested tool is able to process directories.
        // In this case instead of executing the tool with file names, we execute the tool with directories.
        // 
        // In case, when user doesn't want to use directory mode, he needs simply not to pass [wildCardInDirectoryMode] and it will be null
        return warnPluginConfig.wildCardInDirectoryMode?.let {
            handleTestFile(files.map { it.test }.toList(), warnPluginConfig, generalConfig, batchSeparator)
        } ?: run {
            files.chunked(batchSize).flatMap { chunk ->
                handleTestFile(chunk.map { it.test }, warnPluginConfig, generalConfig, batchSeparator)
            }
        }
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<TestFiles> {
        val warnPluginConfig: WarnPluginConfig = testConfig.pluginConfigs.singleIsInstance()
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

    @Suppress(
        "TOO_LONG_FUNCTION",
        "SAY_NO_TO_VAR",
        "LongMethod",
        "ReturnCount",
        "TOO_MANY_LINES_IN_LAMBDA",
        "ComplexMethod"
    )
    private fun handleTestFile(
        originalPaths: List<Path>,
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig,
        batchSeparator: String,
    ): Sequence<TestResult> {
        // extracting all warnings from test resource files
        val copyPaths: List<Path> = createTestFiles(originalPaths, warnPluginConfig)

        // calculate current working dir now, which will be used in SARIF mode later,
        // because during command execution in PB we step out to the different directories,
        // and current directory will be lost
        val workingDirectory = getWorkingDirectory()

        val cmdExecutor = CmdExecutorWarn(
            generalConfig,
            copyPaths,
            extraFlagsExtractor,
            pb,
            generalConfig.execCmd,
            warnPluginConfig.execFlags,
            batchSeparator,
            warnPluginConfig,
            testConfig,
            fs,
        )
        val execCmd = cmdExecutor.constructExecCmd(tmpDirName)

        val expectedWarningsMap = try {
            processExpectedWarnings(generalConfig, warnPluginConfig, originalPaths, copyPaths, workingDirectory)
        } catch (ex: SarifParsingException) {
            return failTestResult(originalPaths, ex, execCmd)
        }

        val executionResult = try {
            cmdExecutor.execCmdAndGetExecutionResults(redirectTo)
        } catch (ex: ProcessTimeoutException) {
            logWarn("The following tests took too long to run and were stopped: $originalPaths, timeout for single test: ${ex.timeoutMillis}")
            return failTestResult(originalPaths, ex, execCmd)
        } catch (ex: ProcessExecutionException) {
            return failTestResult(originalPaths, ex, execCmd)
        }

        val actualWarningsMap = try {
            processActualWarnings(executionResult, warnPluginConfig, originalPaths, workingDirectory)
        } catch (ex: SarifParsingException) {
            return failTestResult(originalPaths, ex, execCmd)
        }

        val resultsChecker = ResultsChecker(
            expectedWarningsMap,
            actualWarningsMap,
            warnPluginConfig,
        )

        return originalPaths.map { path ->
            val resultsStatus = resultsChecker.checkResults(path.name)
            TestResult(
                Test(path),
                resultsStatus.first,
                DebugInfo(
                    execCmd,
                    executionResult.stdout.filter { it.contains(path.name) }.joinToString("\n"),
                    executionResult.stderr.filter { it.contains(path.name) }.joinToString("\n"),
                    null,
                    resultsStatus.second,
                ),
            )
        }.asSequence()
    }

    private fun createTestFiles(paths: List<Path>, warnPluginConfig: WarnPluginConfig): List<Path> {
        logDebug("Creating temp copy files of resources for WarnPlugin...")
        logTrace("Trying to create temp files for: $paths")
        tmpDirName = "${WarnPlugin::class.simpleName!!}-${Random.nextInt()}"
        // don't think that it is really needed now
        val dirPath = constructPathForCopyOfTestFile(tmpDirName, paths[0]).parent!!
        createTempDir(dirPath)

        val ignorePatterns = warnPluginConfig.ignoreLinesPatterns

        return paths.map { path ->
            val copyPath = constructPathForCopyOfTestFile(tmpDirName, path)
            // creating the hierarchy for all files
            fs.createDirectories(copyPath.parent!!)
            fs.write(fs.createFile(copyPath)) {
                fs.readLines(path)
                    .filter { line -> ignorePatterns.none { it.matches(line) } }
                    .map { write((it + "\n").encodeToByteArray()) }
            }
            copyPath
        }
    }

    private fun processExpectedWarnings(
        generalConfig: GeneralConfig,
        warnPluginConfig: WarnPluginConfig,
        originalPaths: List<Path>,
        copyPaths: List<Path>,
        workingDirectory: Path,
    ): WarningMap {
        val expectedWarningsMap = collectExpectedWarnings(generalConfig, warnPluginConfig, originalPaths, copyPaths, workingDirectory)

        if (expectedWarningsMap.isEmpty()) {
            warnMissingExpectedWarnings(warnPluginConfig, generalConfig, originalPaths)
        }
        return expectedWarningsMap
    }

    private fun processActualWarnings(
        executionResult: ExecutionResult,
        warnPluginConfig: WarnPluginConfig,
        originalPaths: List<Path>,
        workingDirectory: Path,
    ): WarningMap {
        val actualResult = if (warnPluginConfig.actualWarningsFormat == ActualWarningsFormat.SARIF &&
                warnPluginConfig.actualWarningsFileName != null) {
            // in this case, after tool execution, there was created sarif report, extract warnings from it,
            // not from stdout
            val sarif = calculatePathToSarifFile(
                sarifFileName = warnPluginConfig.actualWarningsFileName,
                anchorTestFilePath = originalPaths.first()
            )
            ExecutionResult(executionResult.code, fs.readLines(sarif), executionResult.stderr)
        } else {
            // Note: this case is applicable both for PLAIN and SARIF mode, but in the latter case, we suppose,
            // that sarif report is provided in stdout of tool
            executionResult
        }
        return collectActualWarningsWithLineNumbers(actualResult, warnPluginConfig, workingDirectory)
    }

    private fun failTestResult(
        paths: List<Path>,
        ex: Exception,
        execCmd: String
    ) = paths.map {
        TestResult(
            Test(it),
            Fail(ex.describe(), ex.describe()),
            DebugInfo(execCmd, null, ex.message, null, null),
        )
    }.asSequence()

    @Suppress(
        "TooGenericExceptionCaught",
        "SwallowedException",
        "TOO_LONG_FUNCTION"
    )
    private fun collectExpectedWarnings(
        generalConfig: GeneralConfig,
        warnPluginConfig: WarnPluginConfig,
        originalPaths: List<Path>,
        copyPaths: List<Path>,
        workingDirectory: Path,
    ): WarningMap {
        val expectedWarningsFileName: String by lazy {
            warnPluginConfig.expectedWarningsFileName
                ?: throw IllegalArgumentException("<expectedWarningsFileName> is not provided for expectedWarningsFormat=${warnPluginConfig.expectedWarningsFormat}")
        }
        return when (warnPluginConfig.expectedWarningsFormat) {
            ExpectedWarningsFormat.PLAIN -> {
                val warningsFromPlain = collectWarningsFromPlain(expectedWarningsFileName, originalPaths, fs) { line ->
                    with(warnPluginConfig) {
                        line.extractWarning(
                            generalConfig.expectedWarningsPattern!!,
                            fileNameCaptureGroup!!,
                            lineCaptureGroup,
                            columnCaptureGroup,
                            messageCaptureGroup!!,
                            benchmarkMode!!,
                        )
                    }
                }
                copyPaths.associate { copyPath ->
                    copyPath.name to warningsFromPlain.filter { it.fileName == copyPath.name }
                }
            }
            ExpectedWarningsFormat.SARIF -> {
                val warningsFromSarif = try {
                    collectWarningsFromSarif(expectedWarningsFileName, originalPaths, fs, workingDirectory)
                } catch (e: Exception) {
                    throw SarifParsingException("We failed to parse sarif. Check the your tool generation of sarif report, cause: ${e.message}", e.cause)
                }
                copyPaths.associate { copyPath ->
                    copyPath.name to warningsFromSarif.filter { it.fileName == copyPath.name }
                }
            }
            else -> copyPaths.associate { copyPath ->
                val warningsForCurrentPath =
                        copyPath.collectExpectedWarningsWithLineNumbers(
                            warnPluginConfig,
                            generalConfig
                        )
                copyPath.name to warningsForCurrentPath
            }
        }
    }

    /**
     * method for getting expected warnings from test files:
     * 1) reading the file
     * 2) for each line get the warning
     */
    @Suppress("AVOID_NULL_CHECKS")
    private fun Path.collectExpectedWarningsWithLineNumbers(
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig,
    ): List<Warning> = when {
        generalConfig.expectedWarningsEndPattern != null -> collectionMultilineWarnings(
            warnPluginConfig,
            generalConfig,
            fs.readLines(this),
            this,
        )
        else -> collectionSingleWarnings(
            warnPluginConfig,
            generalConfig,
            fs.readLines(this),
            this,
        )
    }

    /**
     * method for getting actual warnings from test files:
     * 1) reading the file
     * 2) for each line get the warning
     */
    @Suppress(
        "AVOID_NULL_CHECKS",
        "TooGenericExceptionCaught",
        "SwallowedException",
    )
    private fun collectActualWarningsWithLineNumbers(
        result: ExecutionResult,
        warnPluginConfig: WarnPluginConfig,
        workingDirectory: Path,
    ): WarningMap = when (warnPluginConfig.actualWarningsFormat) {
        ActualWarningsFormat.SARIF -> try {
            Json.decodeFromString<SarifSchema210>(
                result.stdout.joinToString("\n")
            )
                // setting emptyList() here instead of originalPaths to avoid invalid mapping
                .toWarnings(testConfig.getRootConfig().directory, emptyList(), workingDirectory)
                .groupBy { it.fileName }
                .mapValues { (_, warning) -> warning.sortedBy { it.message } }
        } catch (e: Exception) {
            throw SarifParsingException("Failed to parse sarif. Check the your tool generation of sarif report, cause: ${e.message}", e.cause)
        }

        else -> result.stdout.mapNotNull {
            with(warnPluginConfig) {
                it.extractWarning(
                    actualWarningsPattern!!,
                    fileNameCaptureGroupOut!!,
                    lineCaptureGroupOut,
                    columnCaptureGroupOut,
                    messageCaptureGroupOut!!,
                    benchmarkMode!!
                )
            }
        }
            .groupBy { it.fileName }
            .mapValues { (_, warning) -> warning.sortedBy { it.message } }
    }

    private fun warnMissingExpectedWarnings(
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig,
        originalPaths: List<Path>,
    ) {
        when (warnPluginConfig.expectedWarningsFormat) {
            ExpectedWarningsFormat.IN_PLACE ->
                logWarn(
                    "No expected warnings were found using the following regex pattern:" +
                            " [${generalConfig.expectedWarningsPattern}] in the test files: $originalPaths." +
                            " If you have expected any warnings - please check 'expectedWarningsPattern' or capture groups" +
                            " in your 'save.toml' configuration"
                )
            ExpectedWarningsFormat.SARIF ->
                logWarn(
                    "No expected warnings were found when inspecting files ${warnPluginConfig.expectedWarningsFileName}" +
                            " for test files: $originalPaths." +
                            " If you have expected any warnings - please make sure SARIF files exist, have correct name and contain" +
                            " relevant warnings."
                )
            else -> {
                // this is a generated else block
            }
        }
    }
}
