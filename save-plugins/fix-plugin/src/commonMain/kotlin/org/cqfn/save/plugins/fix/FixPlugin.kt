package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.createRelativePathToTheRoot
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.plugin.ExtraFlags
import org.cqfn.save.core.plugin.ExtraFlagsExtractor
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.resolvePlaceholdersFrom
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.PathSerializer
import org.cqfn.save.core.utils.ProcessExecutionException

import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.Delta
import io.github.petertrr.diffutils.patch.DeltaType
import io.github.petertrr.diffutils.patch.Patch
import io.github.petertrr.diffutils.text.DiffRowGenerator
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

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
    redirectTo) {
    private val diffGenerator = DiffRowGenerator.create()
        .showInlineDiffs(true)
        .mergeOriginalRevised(false)
        .inlineDiffByWord(false)
        .oldTag { start -> if (start) "[" else "]" }
        .newTag { start -> if (start) "<" else ">" }
        .build()
    private lateinit var extraFlagsExtractor: ExtraFlagsExtractor

    @Suppress("TOO_LONG_FUNCTION")
    override fun handleFiles(files: Sequence<TestFiles>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()
        val fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixPluginConfig>().single()
        val generalConfig = testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single()
        extraFlagsExtractor = ExtraFlagsExtractor(generalConfig, fs)

        return files.map { it as FixTestFiles }.chunked(fixPluginConfig.batchSize!!.toInt()).map { chunk ->

            val extraFlagsList = chunk.map { it.test }.mapNotNull { path ->
                extraFlagsExtractor.extractExtraFlagsFrom(path)
            }
                .distinct()
            require(extraFlagsList.size <= 1) {
                "Extra flags for all files in a batch should be same, but you have batchSize=${fixPluginConfig.batchSize}" +
                        " and there are ${extraFlagsList.size} different sets of flags inside it, namely $extraFlagsList"
            }
            val extraFlags = extraFlagsList.singleOrNull() ?: ExtraFlags("", "")

            val pathMap = chunk.map { it.test to it.expected }
            val pathCopyMap = pathMap.map { (test, expected) -> createTestFile(test, generalConfig) to expected }
            val testCopyNames =
                    pathCopyMap.joinToString(separator = fixPluginConfig.batchSeparator!!) { (testCopy, _) -> testCopy.toString() }

            val execFlagsAdjusted = resolvePlaceholdersFrom(fixPluginConfig.execFlags, extraFlags, testCopyNames)
            val execCmd = "${generalConfig.execCmd} $execFlagsAdjusted"
            val time = generalConfig.ms!!.times(pathMap.size)

            val executionResult = try {
                exec(execCmd, testConfig.getRootConfig().directory.toString(), time, chunk.map { it.test })
            } catch (ex: ProcessExecutionException) {
                return@map chunk.map {
                    TestResult(
                        it,
                        Fail(ex.describe(), ex.describe()),
                        DebugInfo(null, ex.message, null)
                    )
                }
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
                        stdout.filter { it.contains(testCopy.name) }.joinToString("\n"),
                        stderr.filter { it.contains(testCopy.name) }.joinToString("\n"),
                        null
                    )
                )
            }
        }
            .flatten()
    }

    private fun checkStatus(expectedLines: List<String>, fixedLines: List<String>) =
            diff(expectedLines, fixedLines).let { patch ->
                if (patch.deltas.isEmpty()) {
                    Pass(null)
                } else {
                    Fail(patch.formatToString(), patch.formatToShortString())
                }
            }

    private fun createTestFile(path: Path, generalConfig: GeneralConfig): Path {
        val pathCopy: Path = constructPathForCopyOfTestFile(FixPlugin::class.simpleName!!, path)
        createTempDir(pathCopy.parent!!)

        val expectedWarningPattern = generalConfig.expectedWarningsPattern

        fs.write(fs.createFile(pathCopy)) {
            fs.readLines(path).forEach {
                if (expectedWarningPattern == null || !generalConfig.expectedWarningsPattern!!.matches(it)) {
                    write(
                        (it + "\n").encodeToByteArray()
                    )
                }
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
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixPlugin::class.simpleName!!)
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
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
