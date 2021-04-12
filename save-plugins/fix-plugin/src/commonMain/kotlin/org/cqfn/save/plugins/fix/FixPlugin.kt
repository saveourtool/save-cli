package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.SaveConfig
import org.cqfn.save.core.config.TestSuiteConfig
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

    override fun execute(saveConfig: SaveConfig, testSuiteConfig: TestSuiteConfig) {
        val fixPluginConfig = testSuiteConfig.pluginConfigs.filterIsInstance<FixPluginConfig>().single()
        discoverFilePairs(fixPluginConfig.testResources)
            .also {
                logInfo("Discovered the following file pairs: $it")
            }
            .forEach { (expected, test) ->
                val original = FileSystem.SYSTEM.readLines(test)
                pb.exec(fixPluginConfig.execCmd.split(" "), "output".toPath())
                val fixed = original  // todo: read result of exec
                val expected = FileSystem.SYSTEM.readLines(expected)
                diff(expected, fixed)
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
