package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.findAllFilesMatching
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.plugins.fix.FixPluginConfig.Companion.defaultResourceNamePattern

import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.Patch
import io.github.petertrr.diffutils.text.DiffRowGenerator
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * A plugin that runs an executable on a file and compares output with expected output.
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")
class FixPlugin : Plugin {
    private val pb = ProcessBuilder()
    private val diffGenerator = DiffRowGenerator.create()
        .showInlineDiffs(true)
        .mergeOriginalRevised(false)
        .inlineDiffByWord(false)
        .oldTag { start -> if (start) "[" else "]" }
        .newTag { start -> if (start) "<" else ">" }
        .build()

    override fun execute(testConfig: TestConfig): Sequence<TestResult> {
        val fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixPluginConfig>().single()
        val files = discoverTestFiles(testConfig.directory)
            .also {
                logInfo("Discovered the following file pairs for comparison: $it")
            }
        return files
            .map { it.first() to it.last() }
            .map { (expected, test) ->
                val executionResult = pb.exec(fixPluginConfig.execCmd, null, false)
                val fixedLines = FileSystem.SYSTEM.readLines(
                    if (fixPluginConfig.inPlace) test else test.parent!! / fixPluginConfig.destinationFileFor(test).toPath()
                )
                val expectedLines = FileSystem.SYSTEM.readLines(expected)
                val status = diff(expectedLines, fixedLines).let { patch ->
                    if (patch.deltas.isEmpty()) {
                        Pass
                    } else {
                        Fail(patch.formatToString())
                    }
                }
                TestResult(
                    listOf(expected, test),
                    status,
                    // todo: fill debug info
                    DebugInfo(executionResult.stdout.joinToString("\n"), null, null)
                )
            }
    }

    override fun discoverTestFiles(root: Path): Sequence<List<Path>> = root
        .findAllFilesMatching { true }
        .asSequence()
        .flatMap { files ->
            files.groupBy {
                val matchResult = defaultResourceNamePattern.matchEntire(it.name)
                matchResult?.groupValues?.get(1)  // this is a capture group for the start of file name
            }
                .filter { it.value.size > 1 && it.key != null }
                .mapValues { (name, group) ->
                    require(group.size == 2) { "Files should be grouped in pairs, but for name $name these files have been discovered: $group" }
                    listOf(
                        group.first { it.name.contains("Expected.") },
                        group.first { it.name.contains("Test.") }
                    )
                }
                .values
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
}
