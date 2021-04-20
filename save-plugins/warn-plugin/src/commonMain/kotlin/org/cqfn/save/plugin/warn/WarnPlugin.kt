package org.cqfn.save.plugin.warn

import okio.FileSystem
import okio.Path
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.plugin.Plugin

/**
 * A plugin that runs an executable and verifies that it produces required warning messages.
 */
class WarnPlugin : Plugin {
    private val fs = FileSystem.SYSTEM

    override fun execute(saveProperties: SaveProperties, testConfig: TestConfig) {
        val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().single()
        val warningRegex = warningRegex(warnPluginConfig)
        discoverTestFiles(warnPluginConfig.testResources).forEach { testFile ->
            val expectedWarnings = fs.readLines(testFile)
                .filter {
                    it.contains(warningRegex)
                }
        }
    }

    internal fun discoverTestFiles(resources: List<Path>) = resources
        .filter { it.name.contains("Test.") }
}
