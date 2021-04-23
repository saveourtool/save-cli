package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.utils.ProcessBuilder

import io.github.petertrr.diffutils.diff
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * A plugin that runs an executable on a file and compares output with expected output.
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")
class FixPlugin : Plugin {
    private val pb = ProcessBuilder()

    override fun execute(saveProperties: SaveProperties, testConfig: TestConfig) {
        val fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixPluginConfig>().single()
        discoverFilePairs(fixPluginConfig.testResources)
            .also {
                logInfo("Discovered the following file pairs for comparison: $it")
            }
            .forEach { (expected, test) ->
                pb.exec(fixPluginConfig.execCmd.split(" "), null, false)
                val fixedLines = FileSystem.SYSTEM.readLines(
                    if (fixPluginConfig.inPlace) test else test.parent!! / fixPluginConfig.destinationFileFor(test).toPath()
                )
                val expectedLines = FileSystem.SYSTEM.readLines(expected)
                // todo: check equality here
                diff(expectedLines, fixedLines)
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
}
