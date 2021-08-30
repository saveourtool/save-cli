package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.ProcessExecutionException

import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.Delta
import io.github.petertrr.diffutils.patch.DeltaType
import io.github.petertrr.diffutils.patch.Patch
import io.github.petertrr.diffutils.text.DiffRowGenerator
import okio.FileSystem
import okio.Path

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

    @Suppress("TOO_LONG_FUNCTION")
    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()

        val fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixPluginConfig>().single()
        val generalConfig = testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single()

        return files.chunked(fixPluginConfig.batchSize!!.toInt()).map { chunk ->
            val pathMap = chunk.map { it.first() to it.last() }
            val pathCopyMap = pathMap.map { (expected, test) -> expected to createTestFile(test, generalConfig) }
            val testCopyNames =
                    pathCopyMap.joinToString(separator = fixPluginConfig.batchSeparator!!) { (_, testCopy) -> testCopy.toString() }

            val execCmd = "${(generalConfig.execCmd)} ${fixPluginConfig.execFlags} $testCopyNames"
            val executionResult = try {
                pb.exec(execCmd, redirectTo)
            } catch (ex: ProcessExecutionException) {
                return@map chunk.map {
                    TestResult(
                        pathMap.map { (expected, test) -> listOf(expected, test) }.flatten(),
                        Fail(ex.describe(), ex.describe()),
                        DebugInfo(null, ex.message, null)
                    )
                }
            }

            val stdout = executionResult.stdout
            val stderr = executionResult.stderr

            pathCopyMap.map { (expected, testCopy) ->
                val fixedLines = fs.readLines(testCopy)
                val expectedLines = fs.readLines(expected)

                val test = pathMap.first { (_, test) -> test.name == testCopy.name }.second

                TestResult(
                    listOf(expected, test),
                    checkStatus(expectedLines, fixedLines),
                    DebugInfo(
                        stdout.filter { it.contains(testCopy.name) }.joinToString("\n"),
                        stderr.filter { it.contains(testCopy.name) }.joinToString("\n"),
                        null
                    )
                )
            }
        }.flatten()
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

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<List<Path>> {
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
                        listOf(
                            group.first { it.name.contains("$resourceNameExpected.") },
                            group.first { it.name.contains("$resourceNameTest.") }
                        )
                    }
                    .values
            }
            .filter { it.isNotEmpty() }
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
}
