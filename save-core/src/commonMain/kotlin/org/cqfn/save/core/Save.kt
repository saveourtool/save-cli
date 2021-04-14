package org.cqfn.save.core

import org.cqfn.save.core.config.SaveConfig
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.plugin.Plugin

import okio.Path.Companion.toPath

/**
 * @property saveCliConfig an instance of [SaveCliConfig]
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")  // todo: remove when there are >1 constructor parameters
class Save(
    private val saveCliConfig: SaveConfig
) {
    /**
     * Main entrypoint for SAVE framework. Discovers plugins and calls their execution.
     */
    fun performAnalysis() {
        // get all toml configs in file system
        val testSuiteConfig = ConfigDetector().configFromFile(saveCliConfig.configPath?.toPath()!!)
        requireNotNull(testSuiteConfig) { "Provided path ${saveCliConfig.configPath} doesn't correspond to a valid save.toml file" }

        val plugins: List<Plugin> = emptyList()  // todo: discover plugins
        plugins.forEach {
            it.execute(testSuiteConfig)
        }
    }
}
