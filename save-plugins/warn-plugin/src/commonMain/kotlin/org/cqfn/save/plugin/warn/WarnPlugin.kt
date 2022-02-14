package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.ActualWarningsFormat
import org.cqfn.save.core.config.ExpectedWarningsFormat
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logTrace
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.ExtraFlagsExtractor
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.ExecutionResult
import org.cqfn.save.core.utils.ProcessExecutionException
import org.cqfn.save.core.utils.ProcessTimeoutException
import org.cqfn.save.plugin.warn.sarif.toWarnings
import org.cqfn.save.plugin.warn.utils.CmdExecutorWarn
import org.cqfn.save.plugin.warn.utils.ResultsChecker
import org.cqfn.save.plugin.warn.utils.Warning
import org.cqfn.save.plugin.warn.utils.collectWarningsFromSarif
import org.cqfn.save.plugin.warn.utils.collectionMultilineWarnings
import org.cqfn.save.plugin.warn.utils.collectionSingleWarnings
import org.cqfn.save.plugin.warn.utils.extractWarning
import org.cqfn.save.plugin.warn.utils.getLineNumber

import io.github.detekt.sarif4k.SarifSchema210
import okio.FileSystem
import okio.Path

import kotlin.random.Random
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
    private lateinit var tmpDirName: String

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
        generalConfig: GeneralConfig
    ): Sequence<TestResult> {
        // extracting all warnings from test resource files
        val copyPaths: List<Path> = createTestFiles(originalPaths, warnPluginConfig)

        val expectedWarningsMap = collectExpectedWarnings(generalConfig, warnPluginConfig, originalPaths, copyPaths)

        if (expectedWarningsMap.isEmpty()) {
            warnMissingExpectedWarnings(warnPluginConfig, generalConfig, originalPaths)
        }

        val cmdExecutor = CmdExecutorWarn(
            generalConfig,
            copyPaths,
            extraFlagsExtractor,
            pb,
            warnPluginConfig,
            testConfig,
            fs,
        )

        val execCmd = cmdExecutor.constructExecCmd(tmpDirName)

        val result = try {
            cmdExecutor.execCmdAndGetExecutionResults(redirectTo)
        } catch (ex: ProcessTimeoutException) {
            logWarn("The following tests took too long to run and were stopped: $originalPaths, timeout for single test: ${ex.timeoutMillis}")
            return failTestResult(originalPaths, ex, execCmd)
        } catch (ex: ProcessExecutionException) {
            return failTestResult(originalPaths, ex, execCmd)
        }

        val actualWarningsMap = collectActualWarningsWithLineNumbers(result, warnPluginConfig)

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
                    result.stdout.filter { it.contains(path.name) }.joinToString("\n"),
                    result.stderr.filter { it.contains(path.name) }.joinToString("\n"),
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

    private fun failTestResult(
        paths: List<Path>,
        ex: ProcessExecutionException,
        execCmd: String
    ) = paths.map {
        TestResult(
            Test(it),
            Fail(ex.describe(), ex.describe()),
            DebugInfo(execCmd, null, ex.message, null, null),
        )
    }.asSequence()

    private fun collectExpectedWarnings(
        generalConfig: GeneralConfig,
        warnPluginConfig: WarnPluginConfig,
        originalPaths: List<Path>,
        copyPaths: List<Path>
    ): WarningMap = if (warnPluginConfig.expectedWarningsFormat == ExpectedWarningsFormat.SARIF) {
        val sarifWarnings = collectWarningsFromSarif(warnPluginConfig, originalPaths, fs)
        copyPaths.associate { copyPath ->
            copyPath.name to sarifWarnings.filter { it.fileName == copyPath.name }
        }
    } else {
        copyPaths.associate { copyPath ->
            val warningsForCurrentPath =
                    copyPath.collectExpectedWarningsWithLineNumbers(
                        warnPluginConfig,
                        generalConfig
                    )
            copyPath.name to warningsForCurrentPath
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
    @Suppress("AVOID_NULL_CHECKS")
    private fun collectActualWarningsWithLineNumbers(
        result: ExecutionResult,
        warnPluginConfig: WarnPluginConfig,
    ): WarningMap = when (warnPluginConfig.actualWarningsFormat) {
        ActualWarningsFormat.SARIF -> Json.decodeFromString<SarifSchema210>(
            result.stdout.joinToString("\n")
        )
            // setting emptyList() here instead of originalPaths to avoid invalid mapping
            .toWarnings(testConfig.getRootConfig().directory, emptyList())
            .groupBy { it.fileName }
            .mapValues { (_, warning) -> warning.sortedBy { it.message } }

        else -> result.stdout.mapNotNull {
            with(warnPluginConfig) {
                it.extractWarning(
                    actualWarningsPattern!!,
                    fileNameCaptureGroupOut!!,
                    it.getLineNumber(actualWarningsPattern, lineCaptureGroupOut),
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
