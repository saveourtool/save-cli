package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.ProcessBuilder

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

    override fun execute(saveProperties: SaveProperties, testConfig: TestConfig): Sequence<TestResult> {
        val fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixPluginConfig>().single()
        val files = discoverFilePairs(fixPluginConfig.testResources)
            .also {
                logInfo("Discovered the following file pairs for comparison: $it")
            }
        return sequence {
            files.forEach { (expected, test) ->
                val executionResult = pb.exec(fixPluginConfig.execCmd.split(" "), null)
                val fixedLines = FileSystem.SYSTEM.readLines(
                    if (fixPluginConfig.inPlace) test else test.parent!! / fixPluginConfig.destinationFileFor(test).toPath()
                )
                val expectedLines = FileSystem.SYSTEM.readLines(expected)
                println("expectedLines: $expectedLines")
                println("fixedLines: $fixedLines")
                val status = diff(expectedLines, fixedLines).let { patch ->
                    if (patch.deltas.isEmpty()) {
                        Pass
                    } else {
                        Fail(patch.formatToString())
                    }
                }
                yield(TestResult(
                    listOf(expected, test),
                    status,
                    // todo: fill debug info
                    DebugInfo(executionResult.stdout.joinToString("\n"), null, null)
                ))
            }
        }
    }

    /**
     * @param resources paths to test resources, where pairs of test/expected files reside.
     * @return list of pairs of corresponding test/expected files.
     */
    internal fun discoverFilePairs(resources: List<Path>) = resources.groupBy { it.parent }
        .flatMap { (_, files) ->
            files
                .filter { it.name.contains("Test.") || it.name.contains("Expected.") }
                .groupBy {
                    it.name.replace("Test", "").replace("Expected", "")
                }
                .filter { it.value.size > 1 }
                .mapValues { (name, group) ->
                    require(group.size == 2) { "Files should be grouped in pairs, but for name $name these files have been discovered: $group" }
                    group.first { it.name.contains("Expected.") } to group.first { it.name.contains("Test.") }
                }
                .values
        }

    private fun Patch<String>.formatToString() = deltas.joinToString("\n") { delta ->
        when (delta) {
            is ChangeDelta -> diffGenerator
                .generateDiffRows(delta.source.lines, delta.target.lines)
                .joinToString("\n") { it.oldLine }
                .let { "[ChangeDelta, position ${delta.source.position}, lines: [$it]]" }
            else -> delta.toString()
        }
    }
}
