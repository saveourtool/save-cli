package com.saveourtool.save.plugins.fix

import com.saveourtool.save.core.config.EvaluatedToolConfig
import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.createFile
import com.saveourtool.save.core.files.createRelativePathToTheRoot
import com.saveourtool.save.core.files.myDeleteRecursively
import com.saveourtool.save.core.files.readLines
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.logging.logWarn
import com.saveourtool.save.core.plugin.ExtraFlags
import com.saveourtool.save.core.plugin.ExtraFlagsExtractor
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.plugin.resolvePlaceholdersFrom
import com.saveourtool.save.core.result.DebugInfo
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestResult
import com.saveourtool.save.core.utils.PathSerializer
import com.saveourtool.save.core.utils.ProcessExecutionException
import com.saveourtool.save.core.utils.ProcessTimeoutException

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

/**
 * A plugin that runs an executable on a file and compares output with expected output.
 * @property testConfig
 */
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

    // fixme: consider refactoring under https://github.com/saveourtool/save/issues/156
    // fixme: should not be common for a class instance during https://github.com/saveourtool/save/issues/28
    private var tmpDirectory: Path? = null
    private lateinit var extraFlagsExtractor: ExtraFlagsExtractor

    @Suppress("TOO_LONG_FUNCTION")
    override fun handleFiles(evaluatedToolConfig: EvaluatedToolConfig, files: Sequence<TestFiles>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()
        val fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixPluginConfig>().single()
        val generalConfig = testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single()
        extraFlagsExtractor = ExtraFlagsExtractor(generalConfig, fs)

        return files.map { it as FixTestFiles }
            .chunked(evaluatedToolConfig.batchSize)
            .map { chunk ->
                val copyPaths = chunk.map { it.test }

                val extraFlagsList = copyPaths.mapNotNull { path -> extraFlagsExtractor.extractExtraFlagsFrom(path) }.distinct()
                require(extraFlagsList.size <= 1) {
                    "Extra flags for all files in a batch should be same, but you have batchSize=${evaluatedToolConfig.batchSize}" +
                            " and there are ${extraFlagsList.size} different sets of flags inside it, namely $extraFlagsList"
                }
                val extraFlags = extraFlagsList.singleOrNull() ?: ExtraFlags("", "")

                val pathMap = chunk.map { it.test to it.expected }
                val pathCopyMap = pathMap.map { (test, expected) ->
                    createTestFile(test, generalConfig, fixPluginConfig) to expected
                }
                val testCopyNames =
                        pathCopyMap.joinToString(separator = evaluatedToolConfig.batchSeparator) { (testCopy, _) -> testCopy.toString() }

                val execFlags = evaluatedToolConfig.execFlags ?: fixPluginConfig.execFlags
                val execFlagsAdjusted = resolvePlaceholdersFrom(execFlags, extraFlags, testCopyNames)
                val execCmdWithoutFlags = evaluatedToolConfig.execCmd ?: generalConfig.execCmd
                val execCmd = "$execCmdWithoutFlags $execFlagsAdjusted"
                val time = generalConfig.timeOutMillis!!.times(pathMap.size)

                val executionResult = try {
                    pb.exec(execCmd, testConfig.getRootConfig().directory.toString(), redirectTo, time)
                } catch (ex: ProcessTimeoutException) {
                    logWarn("The following tests took too long to run and were stopped: ${chunk.map { it.test }}, timeout for single test: ${ex.timeoutMillis}")
                    return@map failTestResult(chunk, ex, execCmd)
                } catch (ex: ProcessExecutionException) {
                    return@map failTestResult(chunk, ex, execCmd)
                }

                val stdout = executionResult.stdout
                val stderr = executionResult.stderr

                pathCopyMap.map { (testCopy, expected) ->
                    val fixedLines = fs.readLines(testCopy)
                    val expectedLines = fs.readLines(expected)

                    val test = pathMap.first { (test, _) -> test.name == testCopy.name }.first

                    TestResult(
                        FixTestFiles(test, expected),
                        checkStatus(expectedLines, fixedLines),
                        DebugInfo(
                            execCmd,
                            stdout.filter { it.contains(testCopy.name) }.joinToString("\n"),
                            stderr.filter { it.contains(testCopy.name) }.joinToString("\n"),
                            null
                        )
                    )
                }
            }
            .flatten()
    }

    private fun failTestResult(
        chunk: List<FixTestFiles>,
        ex: ProcessExecutionException,
        execCmd: String
    ) = chunk.map {
        TestResult(
            it,
            Fail(ex.describe(), ex.describe()),
            DebugInfo(execCmd, null, ex.message, null)
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

    private fun createTestFile(
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
