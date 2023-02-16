package com.saveourtool.save.plugins.fix

import com.saveourtool.save.core.config.ActualFixFormat
import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.createFile
import com.saveourtool.save.core.files.createRelativePathToTheRoot
import com.saveourtool.save.core.files.myDeleteRecursively
import com.saveourtool.save.core.files.readLines
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logWarn
import com.saveourtool.save.core.plugin.ExtraFlags
import com.saveourtool.save.core.plugin.ExtraFlagsExtractor
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.plugin.resolvePlaceholdersFrom
import com.saveourtool.save.core.result.CountWarnings
import com.saveourtool.save.core.result.DebugInfo
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestResult
import com.saveourtool.save.core.utils.PathSerializer
import com.saveourtool.save.core.utils.ProcessExecutionException
import com.saveourtool.save.core.utils.ProcessTimeoutException
import com.saveourtool.save.core.utils.calculatePathToSarifFile
import com.saveourtool.save.core.utils.singleIsInstance

import com.saveourtool.sarifutils.adapter.SarifFixAdapter
import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.Delta
import io.github.petertrr.diffutils.patch.DeltaType
import io.github.petertrr.diffutils.patch.Patch
import io.github.petertrr.diffutils.text.DiffRowGenerator
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass

private typealias PathPair = Pair<Path, Path>

/**
 * A plugin that runs an executable on a file and compares output with expected output.
 * @property testConfig
 */
@Suppress("TooManyFunctions")
class FixPlugin(
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
    redirectTo,
) {
    private val diffGenerator = DiffRowGenerator(
        showInlineDiffs = true,
        mergeOriginalRevised = false,
        inlineDiffByWord = false,
        oldTag = { _, start -> if (start) "[" else "]" },
        newTag = { _, start -> if (start) "<" else ">" },
    )

    // fixme: consider refactoring under https://github.com/saveourtool/save-cli/issues/156
    // fixme: should not be common for a class instance during https://github.com/saveourtool/save-cli/issues/28
    private var tmpDirectory: Path? = null
    private lateinit var extraFlagsExtractor: ExtraFlagsExtractor

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    override fun handleFiles(files: Sequence<TestFiles>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()
        val fixPluginConfig: FixPluginConfig = testConfig.pluginConfigs.singleIsInstance()
        val generalConfig: GeneralConfig = testConfig.pluginConfigs.singleIsInstance()
        val batchSize = requireNotNull(generalConfig.batchSize) {
            "`batchSize` is not set"
        }.toInt()
        val batchSeparator = requireNotNull(generalConfig.batchSeparator) {
            "`batchSeparator` is not set"
        }
        extraFlagsExtractor = ExtraFlagsExtractor(generalConfig, fs)

        return files.map { it as FixTestFiles }
            .chunked(batchSize)
            .map { chunk ->
                val testsPaths = chunk.map { it.test }
                val extraFlags = buildExtraFlags(testsPaths, batchSize)

                val testToExpectedFilesMap = chunk.map { it.test to it.expected }
                val testCopyToExpectedFilesMap = testToExpectedFilesMap.map { (test, expected) ->
                    createCopyOfTestFile(test, generalConfig, fixPluginConfig) to expected
                }

                val execCmd = buildExecCmd(generalConfig, fixPluginConfig, testCopyToExpectedFilesMap, batchSeparator, extraFlags)
                val time = generalConfig.timeOutMillis!!.times(testToExpectedFilesMap.size)

                logDebug("Executing fix plugin in ${fixPluginConfig.actualFixFormat?.name} mode")

                val executionResult = try {
                    pb.exec(execCmd, testConfig.getRootConfig().directory.toString(), redirectTo, time)
                } catch (ex: ProcessTimeoutException) {
                    logWarn("The following tests took too long to run and were stopped: ${chunk.map { it.test }}, timeout for single test: ${ex.timeoutMillis}")
                    return@map failTestResult(chunk, ex, execCmd)
                } catch (ex: ProcessExecutionException) {
                    return@map failTestResult(chunk, ex, execCmd)
                }

                val adjustedTestCopyToExpectedFilesMap = if (fixPluginConfig.actualFixFormat == ActualFixFormat.IN_PLACE) {
                    // hold testCopyToExpectedFilesMap as is
                    testCopyToExpectedFilesMap
                } else {
                    // replace test files with modified copies, obtained from sarif lib
                    val fixedTestCopyToExpectedFilesMap = applyFixesFromSarif(
                        executionResult.stdout,
                        fixPluginConfig,
                        testsPaths,
                        testCopyToExpectedFilesMap
                    )
                    fixedTestCopyToExpectedFilesMap
                }

                val stdout = executionResult.stdout
                val stderr = executionResult.stderr

                buildTestResultsForChunk(
                    testToExpectedFilesMap,
                    adjustedTestCopyToExpectedFilesMap,
                    execCmd,
                    stdout,
                    stderr
                )
            }
            .flatten()
    }

    /**
     * Build additional flags which could be provided directly from text of test file
     *
     * @param testsPaths list of paths of the test files
     * @param batchSize
     * @return [ExtraFlags] instance
     */
    private fun buildExtraFlags(
        testsPaths: List<Path>,
        batchSize: Int,
    ): ExtraFlags {
        val extraFlagsList = testsPaths.mapNotNull { path -> extraFlagsExtractor.extractExtraFlagsFrom(path) }.distinct()
        require(extraFlagsList.size <= 1) {
            "Extra flags for all files in a batch should be same, but you have batchSize=$batchSize" +
                    " and there are ${extraFlagsList.size} different sets of flags inside it, namely $extraFlagsList"
        }
        val extraFlags = extraFlagsList.singleOrNull() ?: ExtraFlags("", "")
        return extraFlags
    }

    /**
     * Build [execCmd] according provided configuration
     *
     * @param generalConfig
     * @param fixPluginConfig
     * @param testCopyToExpectedFilesMap list of paths to the copy of tests files, which will be modificated
     * @param batchSeparator
     * @param extraFlags
     * @return execution command
     */
    private fun buildExecCmd(
        generalConfig: GeneralConfig,
        fixPluginConfig: FixPluginConfig,
        testCopyToExpectedFilesMap: List<PathPair>,
        batchSeparator: String,
        extraFlags: ExtraFlags
    ): String {
        val testsCopyNames = testCopyToExpectedFilesMap.joinToString(separator = batchSeparator) { (testCopy, _) -> testCopy.toString() }
        val execFlags = fixPluginConfig.execFlags
        val execFlagsAdjusted = resolvePlaceholdersFrom(execFlags, extraFlags, testsCopyNames)
        val execCmdWithoutFlags = generalConfig.execCmd
        val execCmd = "$execCmdWithoutFlags $execFlagsAdjusted"
        return execCmd
    }

    /**
     * In this case fixes would be provided by sarif library, which will extract appropriate fixes from SARIF file
     *
     * @param executionResultStdout sarif report data: if sarif file is not provided,
     * supposed, that sarif report is present in stdout of execution result
     * @param fixPluginConfig fix plugin configuration
     * @param testsPaths path to tests file, which need to be modified
     * @param testCopyToExpectedFilesMap list of paths to the copy of tests files, which will be replaced by modified files
     * @return updated list of test files copies
     */
    private fun applyFixesFromSarif(
        executionResultStdout: List<String>,
        fixPluginConfig: FixPluginConfig,
        testsPaths: List<Path>,
        testCopyToExpectedFilesMap: List<PathPair>,
    ): List<PathPair> {
        val tmpDirName = "${FixPlugin::class.simpleName!!}-${Random.nextInt()}".toPath()
        val (sarifFile, isSarifFileEmulated) = getSarifFile(executionResultStdout, fixPluginConfig, testsPaths, tmpDirName)

        // Fixes weren't performed by tool into the test files directly,
        // instead, there was created sarif file with list of fixes, which we will apply ourselves
        val fixedFiles = SarifFixAdapter(
            sarifFile = sarifFile,
            targetFiles = testsPaths
        ).process()

        // sarif file was created by us, remove tmp data
        if (isSarifFileEmulated) {
            fs.deleteRecursively(tmpDirName, mustExist = true)
        }

        // modify existing map, replace test copies to fixed test copies
        val fixedTestCopyToExpectedFilesMap = testCopyToExpectedFilesMap.toMutableList().map { (testCopy, expected) ->
            val fixedTestCopy = fixedFiles.first {
                isComparingTestAndCopy(
                    it,
                    testCopy
                )
            }
            fixedTestCopy to expected
        }
        return fixedTestCopyToExpectedFilesMap
    }

    private fun getSarifFile(
        executionResultStdout: List<String>,
        fixPluginConfig: FixPluginConfig,
        testsPaths: List<Path>,
        tmpDirName: Path,
    ): Pair<Path, Boolean> {
        var isSarifFileEmulated = false

        val sarifFile = if (fixPluginConfig.actualFixSarifFileName != null) {
            // get provided sarif file
            calculatePathToSarifFile(
                sarifFileName = fixPluginConfig.actualFixSarifFileName,
                // Since we have one .sarif file for all tests, just take the first of them as anchor for calculation of paths
                anchorTestFilePath = testsPaths.first()
            )
        } else {
            // sarif report is passed via stdout of executed tool
            // since sarif-utils lib need a real sarif file for processing,
            // emulate it here
            isSarifFileEmulated = true
            createTempDir(tmpDirName)
            val sarifFile = fs.createFile(tmpDirName / "emulated-sarif-report.sarif")
            fs.write(sarifFile) {
                writeUtf8(executionResultStdout.joinToString("\n"))
            }
            sarifFile
        }

        return sarifFile to isSarifFileEmulated
    }

    /**
     * For each chunk, build test results
     *
     * @param testToExpectedFilesMap list of initial test files, necessary for proper report
     * @param testCopyToExpectedFilesMap list of fixed files
     * @param execCmd execution command for debug info
     * @param stdout std out of executed tool, if any
     * @param stderr std err of executed tool, if any
     * @return list of the test results
     */
    private fun buildTestResultsForChunk(
        testToExpectedFilesMap: List<PathPair>,
        testCopyToExpectedFilesMap: List<PathPair>,
        execCmd: String,
        stdout: List<String>,
        stderr: List<String>
    ): List<TestResult> = testCopyToExpectedFilesMap.map { (testCopy, expected) ->
        val fixedLines = fs.readLines(testCopy)
        val expectedLines = fs.readLines(expected)
        val test = testToExpectedFilesMap.first { (test, _) ->
            isComparingTestAndCopy(test, testCopy)
        }.first
        TestResult(
            FixTestFiles(test, expected),
            checkStatus(expectedLines, fixedLines),
            DebugInfo(
                execCmd,
                stdout.filter { it.contains(testCopy.name) }.joinToString("\n"),
                stderr.filter { it.contains(testCopy.name) }.joinToString("\n"),
                null,
                CountWarnings.notApplicable,
            )
        )
    }

    private fun isComparingTestAndCopy(
        test: Path,
        testCopy: Path,
    ): Boolean {
        val testPath = test.trimTmpDir().trimTestRootPath().replaceSeparators()
        val testCopyPath = testCopy.trimTmpDir().trimTestRootPath().replaceSeparators()
        return testCopyPath.compareTo(testPath) == 0
    }

    private fun Path.trimTmpDir(): Path {
        val currentPathAdjusted = this.toString().replaceSeparators()
        val tmpDirAdjusted = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.toString().replaceSeparators()
        return if (currentPathAdjusted.startsWith(tmpDirAdjusted)) {
            currentPathAdjusted
                // trim tmpDir
                .substringAfter("$tmpDirAdjusted/")
                // trim tmp FixPlugin dir
                .substringAfter("/")
                .toPath()
        } else {
            this
        }
    }

    private fun Path.trimTestRootPath(): Path {
        val currentPathAdjusted = this.toString().replaceSeparators()
        val testRootPathAdjusted = testConfig.getRootConfig()
            .directory
            .toString()
            .replaceSeparators()
        return if (currentPathAdjusted.startsWith(testRootPathAdjusted)) {
            currentPathAdjusted
                .substringAfter("$testRootPathAdjusted/")
                .toPath()
        } else {
            this
        }
    }

    private fun String.replaceSeparators(): String = this.replace("\\", "/")

    private fun Path.replaceSeparators(): Path = this.toString().replaceSeparators().toPath()

    private fun failTestResult(
        chunk: List<FixTestFiles>,
        ex: ProcessExecutionException,
        execCmd: String
    ) = chunk.map {
        TestResult(
            it,
            Fail(ex.describe(), ex.describe()),
            DebugInfo(execCmd, null, ex.message, null, CountWarnings.notApplicable)
        )
    }

    private fun checkStatus(expectedLines: List<String>, fixedLines: List<String>) =
            diff(expectedLines, fixedLines).let { patch ->
                if (patch.deltas.isEmpty()) {
                    Pass(null)
                } else {
                    Fail(patch.formatToString(), patch.formatToShortString())
                }
            }

    private fun createCopyOfTestFile(
        path: Path,
        generalConfig: GeneralConfig,
        fixPluginConfig: FixPluginConfig,
    ): Path {
        val pathCopy: Path = constructPathForCopyOfTestFile("${FixPlugin::class.simpleName!!}-${Random.nextInt()}", path)
        tmpDirectory = pathCopy.parent!!
        createTempDir(tmpDirectory!!)

        val defaultIgnoreLinesPatterns: MutableList<Regex> = mutableListOf()
        generalConfig.expectedWarningsPattern?.let { defaultIgnoreLinesPatterns.add(it) }
        generalConfig.runConfigPattern?.let { defaultIgnoreLinesPatterns.add(it) }

        fs.write(fs.createFile(pathCopy)) {
            fs.readLines(path)
                .filter { line ->
                    fixPluginConfig.ignoreLines?.let {
                        fixPluginConfig.ignoreLinesPatterns.none { it.matches(line) }
                    }
                        ?: run {
                            defaultIgnoreLinesPatterns.none { it.matches(line) }
                        }
                }
                .forEach {
                    write(
                        (it + "\n").encodeToByteArray()
                    )
                }
        }
        return pathCopy
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<TestFiles> {
        val fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixPluginConfig>().single()
        val regex = fixPluginConfig.resourceNamePattern
        val resourceNameTest = fixPluginConfig.resourceNameTest
        val resourceNameExpected = fixPluginConfig.resourceNameExpected
        return resourceDirectories
            .map { fs.list(it) }
            .flatMap { files ->
                files.groupBy {
                    val matchResult = (regex).matchEntire(it.name)
                    matchResult?.groupValues?.get(1)  // this is a capture group for the start of file name
                }
                    .filter { it.value.size > 1 && it.key != null }
                    .mapValues { (name, group) ->
                        require(group.size == 2) { "Files should be grouped in pairs, but for name $name these files have been discovered: $group" }
                        FixTestFiles(
                            group.first { it.name.contains("$resourceNameTest.") },
                            group.first { it.name.contains("$resourceNameExpected.") },
                        )
                    }
                    .values
            }
    }

    override fun cleanupTempDir() {
        tmpDirectory?.also {
            if (fs.exists(it)) {
                fs.myDeleteRecursively(it)
            }
        }
    }

    private fun Patch<String>.formatToString() = deltas.joinToString("\n") { delta ->
        when (delta) {
            is ChangeDelta -> diffGenerator
                .generateDiffRows(delta.source.lines, delta.target.lines)
                .joinToString(prefix = "ChangeDelta, position ${delta.source.position}, lines:\n", separator = "\n\n") {
                    """-${it.oldLine}
                      |+${it.newLine}
                      |""".trimMargin()
                }
            else -> delta.toString()
        }
    }

    private fun Patch<String>.formatToShortString(): String = deltas.groupingBy {
        it.type
    }
        .aggregate<Delta<String>, DeltaType, Int> { _, acc, delta, _ ->
            (acc ?: 0) + delta.source.lines.size
        }
        .toList()
        .joinToString { (type, lines) -> "$type: $lines lines" }

    /**
     * @property test test file
     * @property expected expected file
     */
    @Serializable
    data class FixTestFiles(
        @Serializable(with = PathSerializer::class) override val test: Path,
        @Serializable(with = PathSerializer::class) val expected: Path
    ) : TestFiles {
        override fun withRelativePaths(root: Path) = copy(
            test = test.createRelativePathToTheRoot(root).toPath(),
            expected = expected.createRelativePathToTheRoot(root).toPath(),
        )

        companion object {
            /**
             * @param builder `PolymorphicModuleBuilder` to which this class should be registered for serialization
             */
            fun register(builder: PolymorphicModuleBuilder<TestFiles>): Unit =
                    builder.subclass(FixTestFiles::class)
        }
    }
}
