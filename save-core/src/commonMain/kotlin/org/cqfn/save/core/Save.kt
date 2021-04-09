package org.cqfn.save.core

import org.cqfn.save.core.config.SaveConfig
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.plugin.Plugin

/**
 * @property saveConfig an instance of [SaveConfig]
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")  // todo: remove when there are >1 constructor parameters
class Save(
    private val saveConfig: SaveConfig
) {
    /**
     * Main entrypoint for SAVE framework. Discovers plugins and calls their execution.
     */
    fun performAnalysis() {
        // get all toml configs in file system
        val testSuiteConfig = ConfigDetector().configFromFile(saveConfig.configPath)
        requireNotNull(testSuiteConfig) { "Provided path ${saveConfig.configPath} doesn't correspond to a valid save.toml file" }

        val plugins: List<Plugin> = emptyList()  // todo: discover plugins
        plugins.forEach {
            it.execute(testSuiteConfig)
        }
    }
}
