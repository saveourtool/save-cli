package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.ExpectedWarningsFormat
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.ExtraFlagsExtractor
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.ProcessExecutionException
import org.cqfn.save.core.utils.ProcessTimeoutException
import org.cqfn.save.plugin.warn.utils.CmdExecutorWarn
import org.cqfn.save.plugin.warn.utils.ResultsChecker
import org.cqfn.save.plugin.warn.utils.Warning
import org.cqfn.save.plugin.warn.utils.collectWarningsFromSarif
import org.cqfn.save.plugin.warn.utils.collectionMultilineWarnings
import org.cqfn.save.plugin.warn.utils.collectionSingleWarnings
import org.cqfn.save.plugin.warn.utils.extractWarning
import org.cqfn.save.plugin.warn.utils.getLineNumber

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

        val expectedWarningsMap: WarningMap = copyPaths.zip(originalPaths).associate { (copyPath, originalPath) ->
            val warningsForCurrentPath =
                    copyPath.collectWarningsWithLineNumbers(warnPluginConfig, generalConfig, originalPaths, originalPath)
            copyPath.name to warningsForCurrentPath
        }

        if (expectedWarningsMap.isEmpty()) warnMissingExpectedWarnings(warnPluginConfig, generalConfig, originalPaths)

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

        try {
            val result = cmdExecutor.executeCommandAndGetTestResults(redirectTo)

            val actualWarningsMap = result.stdout.mapNotNull {
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
        } catch (ex: ProcessTimeoutException) {
            logWarn("The following tests took too long to run and were stopped: $originalPaths, timeout for single test: ${ex.timeoutMillis}")
            return failTestResult(originalPaths, ex, execCmd)
        } catch (ex: ProcessExecutionException) {
            return failTestResult(originalPaths, ex, execCmd)
        }
    }

    private fun createTestFiles(paths: List<Path>, warnPluginConfig: WarnPluginConfig): List<Path> {
        logDebug("Trying to create temp files for: $paths")
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

    /**
     * method for getting warnings from test files:
     * 1) reading the file
     * 2) for each line get the warning
     */
    @Suppress("AVOID_NULL_CHECKS")
    private fun Path.collectWarningsWithLineNumbers(
        warnPluginConfig: WarnPluginConfig,
        generalConfig: GeneralConfig,
        originalPaths: List<Path>,
        originalPath: Path,
    ): List<Warning> = when {
        warnPluginConfig.expectedWarningsFormat == ExpectedWarningsFormat.SARIF -> collectWarningsFromSarif(
            warnPluginConfig,
            originalPath,
            originalPaths,
            fs,
            this
        )
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
